package server.api.main;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import server.api.APIHandler;
import util.Web;

public class APIHandler_connection_test extends APIHandler {

	public APIHandler_connection_test(Broker broker) {
		super(broker, "connection_test");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		byte[] data = new byte[1024*1024];
		response.setContentType(Web.MIME_BINARY);
		for (int i = 0; i < 20; i++) {
			response.getOutputStream().write(data);				
		}
	}
}
