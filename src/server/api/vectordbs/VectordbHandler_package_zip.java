package server.api.vectordbs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import vectordb.VectorDB;

public class VectordbHandler_package_zip extends VectordbHandler {
	private static final Logger log = LogManager.getLogger();
	
	public VectordbHandler_package_zip(Broker broker) {
		super(broker, "package.zip");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			List<Path> filenames = vectordb.getDataFilenames();
			Path root = vectordb.getDataPath();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			
			for(Path filename:filenames) {
				zipOutputStream.putNextEntry(new ZipEntry(filename.toString()));
				Path filePath = root.resolve(filename);
				Files.copy(filePath, zipOutputStream);
				zipOutputStream.closeEntry();
			}
			
			zipOutputStream.finish();
			zipOutputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
