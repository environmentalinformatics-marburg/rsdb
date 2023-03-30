package server.api.rasterdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import server.api.main.ChunkedUploader;
import server.api.main.ChunkedUploader.ChunkedUpload;
import util.Util;
import util.Web;

public class RasterdbMethod_attachments extends RasterdbMethod {
	
	private static final HashMap<String, String> EXTENSTION_MAP = new HashMap<String, String>();
	
	static {
		EXTENSTION_MAP.put("pdf", Web.MIME_PDF);
		EXTENSTION_MAP.put("txt", Web.MIME_TEXT);
	}
	
	private static final Path TEMP_PATH = Paths.get("temp/raster_attachments");

	ChunkedUploader chunkedUploader = new ChunkedUploader(TEMP_PATH);
	
	public RasterdbMethod_attachments(Broker broker) {
		super(broker, "attachments");
	}
	
	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info(request);
		request.setHandled(true);
		String reqMethod = request.getMethod();
		switch(reqMethod) {
		case "GET":
			handleGET(rasterdb, target, request, response, userIdentity);
			break;
		case "DELETE":
			handleDELETE(rasterdb, target, request, response, userIdentity);
			break;
		case "POST":
			handlePOST(rasterdb, target, request, response, userIdentity);
			break;			
		default:
			throw new RuntimeException("unknown request method: "  + reqMethod);
		}		
	}
	
	public void handleGET(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info(target);
		try {
			Path filePath = rasterdb.getAttachmentFilePath(target);
			response.setStatus(HttpServletResponse.SC_OK);			
			int extensionIndex = target.lastIndexOf('.');
			if (extensionIndex > 0) {
				String extension = target.substring(extensionIndex + 1).toLowerCase();
				String mime = EXTENSTION_MAP.get(extension);
				if(mime != null) {
					response.setContentType(mime);
				} else {
					response.setContentType(Web.MIME_BINARY);
				}
			}			
			Files.copy(filePath, response.getOutputStream());
			
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
	
	private void handleDELETE(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		rasterdb.checkMod(userIdentity);
		Logger.info("handleDELETE " + target);
		rasterdb.removeAttachmentFile(target);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_TEXT);
	}
	
	private void handlePOST(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		rasterdb.checkMod(userIdentity);
		
		Consumer<ChunkedUpload> fileConsumer = chunkedUpload -> {
			synchronized (ChunkedUploader.GLOBAL_LOCK) {
				try {
					chunkedUploader.map.remove(chunkedUpload.filename);
					chunkedUpload.raf.close();
					Path srcPath = chunkedUpload.path;					
					Path root = rasterdb.config.getAttachmentFolderPath();
					Path dstPath = root.resolve(chunkedUpload.filename);
					root.toFile().mkdirs();
					Util.checkSafePath(srcPath);
					Util.checkSafePath(dstPath);
					Logger.info("move " + srcPath +" -> " + dstPath);
					Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (IOException e) {
					try {
						Logger.error(e);
						e.printStackTrace();
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						response.setContentType(Web.MIME_TEXT);
						response.getWriter().println(e.getMessage());
					} catch (IOException e1) {
						Logger.error(e1);
					}
				}
			}		
		};

		chunkedUploader.handle(request, response, fileConsumer);		
	}
}
