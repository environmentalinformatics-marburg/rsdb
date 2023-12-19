package server.api.roi_groups;

import java.io.IOException;
import java.util.Iterator;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.apache.commons.math3.analysis.function.Multiply;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.Roi;
import broker.group.RoiGroup;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.PolygonUtil.PolygonWithHoles;
import util.Web;

public class APIHandler_roi_group {

	private final Broker broker;

	public APIHandler_roi_group(Broker broker) {
		this.broker = broker;
	}

	public void handle(String name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info("get: " + name);
		RoiGroup roiGroup = broker.getRoiGroup(name);
		Logger.info("get: " + roiGroup);
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
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("roi_group");
		json.object();
		json.key("name");
		json.value(roiGroup.name);
		roiGroup.informal.writeJson(json);
		json.key("epsg");
		json.value(roiGroup.epsg);
		json.key("proj4");
		json.value(roiGroup.proj4);
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
			json.key("point_count");
			json.value(PolygonUtil.PolygonsWithHoles.pointCount(r.polygons));
			json.key("polygon");
			json.array();
			if(PolygonUtil.PolygonsWithHoles.isPlainPolygon(r.polygons)) {
				Point2d[] points = r.polygons[0].polygon;
				for(Point2d p : points) {
					json.array();
					json.value(p.x);
					json.value(p.y);
					json.endArray();
				}
			}			
			json.endArray();
			{
				int polyCnt = r.polygons.length;
				int holeCnt = 0;
				for(PolygonWithHoles poly : r.polygons) {
					if(poly.hasHoles()) {
						holeCnt += poly.holes.length;
					}
				}
				String c = "";
				if(polyCnt == 1) {
					if(holeCnt == 0) {
						c = "polygon";
					} else if(holeCnt == 1) {
						c = "polygon with hole";
					} else {
						c = "polygon with " + holeCnt + " holes";
					}
				} else {
					if(holeCnt == 0) {
						c = polyCnt + " polygons";
					} else if(holeCnt == 1) {
						c = polyCnt + " polygons with hole";
					} else {
						c = polyCnt + " polygons with " + holeCnt + " holes";
					}
				}
				json.key("characteristic");
				json.value(c);

				double area = PolygonUtil.PolygonsWithHoles.area(r.polygons);
				if(Double.isFinite(area)) {
					json.key("area");
					json.value(area);
				}
			}
			json.endObject();			
		}
		json.endArray();
		if(roiGroup.hasMessages()) {
			json.key("messages");
			json.array();
			for(String m : roiGroup.messages) {			
				json.value(m);					
			}
			json.endArray();
		}
		//if(EmptyACL.ADMIN.isAllowed(userIdentity)) {
		json.key("acl");
		roiGroup.acl.writeJSON(json);
		//}
		json.endObject(); // roi_group
		json.endObject(); // JSON
	}

}
