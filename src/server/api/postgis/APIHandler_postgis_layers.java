package server.api.postgis;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import postgis.PostgisLayerManager;
import util.Web;

public class APIHandler_postgis_layers {

	private final Broker broker;
	private PostgisLayerManager postgisLayerManager;
	private final APIHandler_postgis_layer apihandler_postgis_layer;

	public APIHandler_postgis_layers(Broker broker) {
		this.broker = broker;
		this.postgisLayerManager = broker.postgisLayerManager();
		apihandler_postgis_layer = new APIHandler_postgis_layer(broker);
	}

	public void handle(String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			if(target.equals("/")) {
				handleList(request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				apihandler_postgis_layer.handle(name, next, request, (Response)response, userIdentity);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e);
		}
	}

	private void handleList(Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("postgis_layers");
		json.array();
		postgisLayerManager.forEach(userIdentity, postgisLayerBase -> {
			json.object();
			json.key("name");
			json.value(postgisLayerBase.name);
			json.endObject();
		});
		json.endArray();
		json.endObject();
	}
}
