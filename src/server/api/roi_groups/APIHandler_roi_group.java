package server.api.roi_groups;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.EmptyACL;
import broker.group.Roi;
import broker.group.RoiGroup;
import pointdb.base.Point2d;

public class APIHandler_roi_group {
	private static final Logger log = LogManager.getLogger();

	protected static final String MIME_JSON = "application/json";

	private final Broker broker;

	public APIHandler_roi_group(Broker broker) {
		this.broker = broker;
	}

	public void handle(String name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.info("get: " + name);
		RoiGroup roiGroup = broker.getRoiGroup(name);
		log.info("get: " + roiGroup);
		if(roiGroup == null) {
			throw new RuntimeException("roi_group not found: " + name);
		} else if(target.equals("/")) {
			switch(request.getMethod()) {
			case "GET":
				roiGroup.acl.check(userIdentity);
				handleGET(roiGroup, request, response, userIdentity);
				break;
			default:
				throw new RuntimeException("invalid HTTP method: " + request.getMethod());
			}
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name in roi_group: " + target);
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

	private void handleGET(RoiGroup roiGroup, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("roi_group");
		json.object();
		json.key("name");
		json.value(roiGroup.name);
		roiGroup.informal.writeJson(json);
		json.key("rois");
		json.array();
		for(Roi r:roiGroup.rois) {
			json.object();
			json.key("name");
			json.value(r.name);
			json.key("center");
			json.array();
			json.value(r.center.x);
			json.value(r.center.y);
			json.endArray();
			json.key("polygon");
			json.array();
			for(Point2d p:r.points) {
				json.array();
				json.value(p.x);
				json.value(p.y);
				json.endArray();
			}
			json.endArray();			
			json.endObject();			
		}
		json.endArray();
		//if(EmptyACL.ADMIN.isAllowed(userIdentity)) {
			json.key("acl");
			roiGroup.acl.writeJSON(json);
		//}
		json.endObject(); // roi_group
		json.endObject(); // JSON
	}
	
}
