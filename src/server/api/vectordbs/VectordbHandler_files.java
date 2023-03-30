package server.api.vectordbs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import server.api.main.ChunkedUploader;
import server.api.main.ChunkedUploader.ChunkedUpload;
import util.Util;
import util.Web;
import vectordb.VectorDB;

public class VectordbHandler_files extends VectordbHandler {
	

	private static final Path TEMP_PATH = Paths.get("temp/vector");

	ChunkedUploader chunkedUploader = new ChunkedUploader(TEMP_PATH);

	public VectordbHandler_files(Broker broker) {
		super(broker, "files");
	}

	@Override
	public void handlePOST(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		vectordb.checkMod(userIdentity);
		
		Consumer<ChunkedUpload> fileConsumer = chunkedUpload -> {
			synchronized (ChunkedUploader.GLOBAL_LOCK) {
				try {
					chunkedUploader.map.remove(chunkedUpload.filename);
					chunkedUpload.raf.close();
					Path srcPath = chunkedUpload.path;
					Path dstPath = vectordb.getDataPath().resolve(chunkedUpload.filename);
					Util.checkSafePath(srcPath);
					Util.checkSafePath(dstPath);
					Logger.info("move " + srcPath +" -> " + dstPath);
					Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					vectordb.refreshDatatag();
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
	
	@Override
	public void handleDELETE(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		vectordb.checkMod(userIdentity);
		
		Logger.info(target);
		vectordb.removeFile(target);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_TEXT);
	}
}
