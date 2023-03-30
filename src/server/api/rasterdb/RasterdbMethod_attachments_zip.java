package server.api.rasterdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import util.Web;

public class RasterdbMethod_attachments_zip extends RasterdbMethod {
	
	public RasterdbMethod_attachments_zip(Broker broker) {
		super(broker, "attachments.zip");
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
		default:
			throw new RuntimeException("unknown request method: "  + reqMethod);
		}		
	}
	
	public void handleGET(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			List<Path> filenames = rasterdb.getAttachmentFilenames();
			Path root = rasterdb.config.getAttachmentFolderPath();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_ZIP);
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);

			for(Path filename : filenames) {
				String entryName = filename.toString();
				zipOutputStream.putNextEntry(new ZipEntry(entryName));
				Path filePath = root.resolve(filename);
				Files.copy(filePath, zipOutputStream);
				zipOutputStream.closeEntry();
			}

			zipOutputStream.finish();
			zipOutputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
