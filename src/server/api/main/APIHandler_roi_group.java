package server.api.main;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.Roi;
import broker.group.RoiGroup;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.PolygonUtil.PolygonsWithHoles;
import server.api.APIHandler;
import util.Web;

public class APIHandler_roi_group extends APIHandler {
	
	public APIHandler_roi_group(Broker broker) {
		super(broker, "roi_group");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		String name = request.getParameter("name");
		if(name==null) {
			throw new RuntimeException("missing name parameter");
		}
		
		RoiGroup roiGroup = broker.getRoiGroup(name);
		roiGroup.acl.check(Web.getUserIdentity(request));
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

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
			json.endObject();			
		}
		json.endArray();		
	}
}
