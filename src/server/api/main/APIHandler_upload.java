package server.api.main;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import server.api.APIHandler;

public class APIHandler_upload extends APIHandler {

	private static final Path TEMP_PATH = Paths.get("temp/raster");

	ChunkedUploader chunkedUploader = new ChunkedUploader(TEMP_PATH);

	public APIHandler_upload(Broker broker) {
		super(broker, "upload");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		chunkedUploader.handle("raster", request, response, null);		
	}	
}
