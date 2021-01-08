package server.api.poi_groups;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.Poi;
import broker.group.PoiGroup;

public class APIHandler_poi_group {
	private static final Logger log = LogManager.getLogger();

	protected static final String MIME_JSON = "application/json";

	private final Broker broker;

	public APIHandler_poi_group(Broker broker) {
		this.broker = broker;
	}

	public void handle(String name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.info("get: " + name);
		PoiGroup poiGroup = broker.getPoiGroup(name);
		log.info("get: " + poiGroup);
		if(poiGroup == null) {
			throw new RuntimeException("poi_group not found: " + name);
		} else if(target.equals("/")) {
			switch(request.getMethod()) {
			case "GET":
				poiGroup.acl.check(userIdentity);
				handleGET(poiGroup, request, response, userIdentity);
				break;
			default:
				throw new RuntimeException("invalid HTTP method: " + request.getMethod());
			}
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name in poi_group: " + target);
			}			
			String resource = i < 0 ? target.substring(1) : target.substring(1, i);
			int formatIndex = resource.lastIndexOf('.');
			String resourceName = formatIndex < 0 ? resource : resource.substring(0, formatIndex);
			String resourceFormat = formatIndex < 0 ? "" : resource.substring(formatIndex + 1);
			String next = i < 0 ? "/" : target.substring(i);
			if(next.equals("/")) {
				switch(resourceName) {					
				default:
					throw new RuntimeException("unknown resource: " + resource);
				}
			} else {
				throw new RuntimeException("error in subpath: " + target);
			}
		}		
	}

	private void handleGET(PoiGroup poiGroup, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("poi_group");
		json.object();
		json.key("name");
		json.value(poiGroup.name);
		poiGroup.informal.writeJson(json);
		json.key("epsg");
		json.value(poiGroup.epsg);
		json.key("proj4");
		json.value(poiGroup.proj4);
		json.key("pois");
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
		//if(EmptyACL.ADMIN.isAllowed(userIdentity)) {
			json.key("acl");
			poiGroup.acl.writeJSON(json);
		//}
		json.endObject(); // poi_group
		json.endObject(); // JSON
	}
	
}
