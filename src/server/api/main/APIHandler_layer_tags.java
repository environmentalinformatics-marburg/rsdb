package server.api.main;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import server.api.APIHandler;

public class APIHandler_layer_tags extends APIHandler {
	//private static final Logger log = LogManager.getLogger();

	public APIHandler_layer_tags(Broker broker) {
		super(broker, "layer_tags");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		//EmptyACL.ADMIN.check(Web.getUserIdentity(request));
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());


		json.object();
		json.key("layer_tags");
		json.value(broker.catalog.getTags());
		json.endObject();
		
		
	}
}
