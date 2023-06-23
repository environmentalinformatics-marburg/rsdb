package server.api.postgis;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.PostgisLayer;
import broker.PostgisLayerManager;
import jakarta.servlet.http.HttpServletResponse;
import util.Util;
import util.Web;

public class APIHandler_postgis_layer {

	private final Broker broker;
	private final PostgisLayerManager layerManager;

	public APIHandler_postgis_layer(Broker broker) {
		this.broker = broker;
		this.layerManager = broker.postgisLayerManager();
	}

	public void handle(String layerName, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			Util.checkStrictID(layerName);	
			if(target.equals("/")) {
				PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
				handleList(postgisLayer, request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(name) {
				default:
					throw new RuntimeException("unknown target");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e);
		}
	}
	
	private void handleList(PostgisLayer postgisLayer, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		
		long count = postgisLayer.getFeatures();
		
		json.object();
		json.key("layer");
		json.value(count);
		json.endObject();
	}
}
