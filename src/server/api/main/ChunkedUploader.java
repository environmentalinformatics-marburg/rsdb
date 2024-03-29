package server.api.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import com.googlecode.javaewah.datastructure.BitSet;

import util.Util;
import util.Web;

public class ChunkedUploader {
	
	public static final Object GLOBAL_LOCK = new Object();
	private static final int FILE_SIZE_THRESHOLD = 100*1024*1024;
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"), -1, -1, FILE_SIZE_THRESHOLD);
	
	public ConcurrentHashMap<String, ChunkedUpload> uploadMap = new ConcurrentHashMap<String, ChunkedUpload>();
	private Path root;
	
	public ChunkedUploader(Path root) {
		this.root = root;
	}	
	
	public static class ChunkedUpload {	
		
		public final String uploadID;
		public final String filename;
		public final Path path;
		public final int chunkSize;
		public final int totalChunks;
		public final long totalSize;
		public final BitSet received;
		public final RandomAccessFile raf;

		public ChunkedUpload(String uploadID, Path root, String filename, int chunkSize, int totalChunks, long totalSize) throws IOException {
			this.uploadID = uploadID;
			this.filename = filename;
			this.path = root.resolve(filename);
			this.chunkSize = chunkSize;
			this.totalChunks = totalChunks;
			this.totalSize = totalSize;
			this.received = new BitSet(totalChunks);			
			Util.checkIsParent(root, path);
			
			if(chunkSize <= 0 || chunkSize > 100*1024*1024) {
				throw new RuntimeException("chunck size error");
			}
			if(totalChunks < 0 || totalChunks > 200_000) {
				throw new RuntimeException("chunck count error");
			}
			if(totalSize < 0 || totalSize > 1024l*1024l*1024l*1024l) {
				throw new RuntimeException("file size error");
			}
			if(filename.isEmpty() || filename.contains("/") || filename.contains("\\")) {
				throw new RuntimeException("invalid filename");
			}
			synchronized(GLOBAL_LOCK) {
				if(Files.exists(path)) {
					Logger.warn("file already exists");
					//throw new RuntimeException("file already exists");
				}
				File file = path.toFile();
				Util.createPathOfFile(file);
				raf = new RandomAccessFile(file, "rw");
			}
			raf.setLength(totalSize);
		}

		public synchronized void add(int chunkNumber, byte[] chunkData) throws IOException {
			int chunkIndex = chunkNumber - 1;
			if(chunkIndex < 0 || chunkIndex >= totalChunks) {
				throw new RuntimeException("chunkIndex out of range: " + chunkIndex);
			}
			if(chunkData.length != chunkSize && chunkIndex != (totalChunks - 1)) {
				throw new RuntimeException("wrong chunkSize");
			}
			long pos = (((long)(chunkIndex) * ((long)chunkSize)));
			if(chunkIndex == (totalChunks - 1) && (pos + chunkData.length) != totalSize) {
				throw new RuntimeException("wrong last chunk: totalSize:" + totalSize + "  pos:" + pos + "  chunkSize:" + chunkData.length+ "  chunkIndex:" + chunkIndex+"  totalChunks:" + totalChunks);
			}
			if(received.get(chunkIndex)) {
				Logger.info("chunk already inserted");
			}

			raf.seek(pos);
			raf.write(chunkData);

			received.set(chunkIndex);
			Logger.info("cardinality "+received.cardinality() + " of " + totalChunks);
		}
		public synchronized boolean isFinished() {
			return received.cardinality() == totalChunks;
		}

		@Override
		public String toString() {
			return "ChunkedUpload [uploadID=" + uploadID + ", filename=" + filename + ", path=" + path + ", chunkSize="
					+ chunkSize + ", totalChunks=" + totalChunks + ", totalSize=" + totalSize + "]";
		}
	}
	
	public void handle(String rootID, Request request, Response response, Consumer<ChunkedUpload> fileConsumer) throws IOException {
		Logger.info(System.getProperty("java.io.tmpdir"));
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_TEXT);
		//Logger.info("handle upload");

		//Logger.info(request.getContentType());
		if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
			request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
		}


		try {
			String filename = null;
			int chunkSize = -1;
			int totalChunks = -1;
			int chunkNumber = -1;
			byte[] chunkData = null;
			int currentChunkSize = -1;
			long totalSize = -1;

			Collection<Part> parts = request.getParts();
			//Logger.info("parts " + parts.size());
			for(Part part:parts) {
				String partName = part.getName();
				switch(partName) {
				case "chunkNumber":
					chunkNumber = toInt(part);
					//Logger.info("chunkNumber " + chunkNumber);
					break;
				case "identifier": //ignore
					//String identifier = toString(part);
					//Logger.info("identifier " + identifier);
					break;
				case "totalSize":
					totalSize = toLong(part);
					Logger.info("totalSize " + totalSize);
					break;
				case "filename":
					filename = toString(part);
					Logger.info("filename " + filename);
					break;
				case "file":
					//Logger.info("file " + toString(part));
					chunkData = toBytes(part);
					break;
				case "chunkSize": //ignore
					chunkSize = toInt(part);
					//Logger.info("chunkSize " + chunkSize);
					break;
				case "relativePath": //ignore
					//String relativePath = toString(part);
					//Logger.info("relativePath " + relativePath);
					break;
				case "totalChunks":
					totalChunks = toInt(part);
					//Logger.info("totalChunks " + totalChunks);
					break;
				case "currentChunkSize":
					currentChunkSize = toInt(part);
					Logger.info("currentChunkSize " + currentChunkSize);
					break;
				default:
					Logger.info("unknown partName " + partName);
				}
			}
			
			if(filename == null) {
				throw new RuntimeException("missuing chunk filename");	
			}
			
			String uploadID = rootID + "/" + filename;

			if(currentChunkSize != chunkData.length) {
				uploadMap.remove(uploadID);
				throw new RuntimeException("chunk read error");	
			}

			ChunkedUpload chunkedUpload = uploadMap.get(uploadID);
			boolean existingChunk = chunkedUpload != null;
			Logger.info("get " + uploadID + " -> " + chunkedUpload);
			if(!existingChunk) {
				chunkedUpload = new ChunkedUpload(uploadID, root, filename, chunkSize, totalChunks, totalSize);
				ChunkedUpload old = uploadMap.putIfAbsent(uploadID, chunkedUpload);
				if(old != null) {
					chunkedUpload = old;
					existingChunk = true;
				}
			} 
			
			if(existingChunk){
				if(!uploadID.equals(chunkedUpload.uploadID) || !filename.equals(chunkedUpload.filename) || chunkSize != chunkedUpload.chunkSize || totalChunks != chunkedUpload.totalChunks || totalSize != chunkedUpload.totalSize) {
					Logger.warn("same uploadID with different upload params, create new upload: " + uploadID);
					chunkedUpload = new ChunkedUpload(uploadID, root, filename, chunkSize, totalChunks, totalSize);
					uploadMap.put(uploadID, chunkedUpload);
				}
			}
			chunkedUpload.add(chunkNumber, chunkData);

			if(chunkedUpload.isFinished()) {
				Logger.info("finished!");
				if(fileConsumer != null) {
					fileConsumer.accept(chunkedUpload);
				}
				uploadMap.remove(uploadID);
			}

		} catch (Exception e) {
			Logger.error(e);
			throw new RuntimeException(e);
		}

		/*HttpInput in = request.getHttpInput();
		long read_bytes = 0;
		while(!in.isFinished()) {
			int c = in.read();
			Logger.info(((char) c));
			read_bytes++;
		}
		Logger.info("read_bytes " + read_bytes);*/
	}
	
	public static byte[] toBytes(Part part) throws IOException {
		int raw_size = (int)part.getSize();
		byte[] raw = new byte[raw_size];
		int pos = 0;
		InputStream in = part.getInputStream();
		while(pos < raw_size) {
			//Logger.info("read at "+pos+" of "+raw_size);
			int read_size = in.read(raw, pos, raw_size - pos);
			if(read_size < 1) {
				throw new RuntimeException("not all bytes read "+pos+"  of  "+raw_size);
			}
			pos += read_size;
		}
		if(pos != raw_size) {
			throw new RuntimeException("not all bytes read "+pos+"  of  "+raw_size);
		}
		return raw;
	}

	public String toString(Part part) throws IOException {
		return new String(toBytes(part));
	}

	public long toLong(Part part) throws NumberFormatException, IOException {
		return Long.parseLong(toString(part));
	}

	public int toInt(Part part) throws NumberFormatException, IOException {
		return Integer.parseInt(toString(part));
	}

}
