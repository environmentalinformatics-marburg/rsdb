package server.api.main;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.Poi;
import broker.group.PoiGroup;
import server.api.APIHandler;
import util.Web;

public class APIHandler_poi_group extends APIHandler {
	
	public APIHandler_poi_group(Broker broker) {
		super(broker, "poi_group");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		String name = request.getParameter("name");
		if(name==null) {
			throw new RuntimeException("missing name parameter");
		}
		
		PoiGroup poiGroup = broker.getPoiGroup(name);
		poiGroup.acl.check(Web.getUserIdentity(request));				
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());		

		json.array();
		for(Poi p:poiGroup.pois) {
			json.object();
			json.key("name");
			json.value(p.name);
			json.key("x");
			json.value(p.x);
			json.key("y");
			json.value(p.y);
			json.endObject();
			
		}
		json.endArray();		
	}
}
