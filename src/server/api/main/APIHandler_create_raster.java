package server.api.main;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import server.api.APIHandler;

public class APIHandler_create_raster extends APIHandler {

	public APIHandler_create_raster(Broker broker) {
		super(broker, "create_raster");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		String name = request.getParameter("name");
		if(name==null) {
			throw new RuntimeException("missing name parameter");
		}
		broker.createOrGetRasterdb(name);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("created raster "+name);
		json.endObject();
	}
}
