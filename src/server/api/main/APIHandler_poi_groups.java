package server.api.main;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.PoiGroup;
import pointdb.PointDB;
import server.api.APIHandler;
import util.Web;

public class APIHandler_poi_groups extends APIHandler {
	

	public APIHandler_poi_groups(Broker broker) {
		super(broker, "poi_groups");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {

		UserIdentity userIdentity = Web.getUserIdentity(request);

		Builder<PoiGroup> builder = Stream.<PoiGroup>builder();

		String pointdbName = request.getParameter("pointdb");

		if(pointdbName!=null) {
			PointDB db = broker.getPointdb(pointdbName);
			db.config.check(userIdentity);
			db.config.getPoiGroupNames().forEach(poiGroupName-> {
				try {
					PoiGroup poiGroup = broker.getPoiGroup(poiGroupName);
					if(poiGroup.acl.isAllowed(userIdentity)) {
						builder.accept(poiGroup);
					}
				} catch(Exception e) {
					Logger.warn(e);
				}
			});
		} else {
			broker.getPoiGroups().forEach(poiGroup->{
				if(poiGroup.acl.isAllowed(userIdentity)) {
					builder.accept(poiGroup);
				}
			});
		}

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.array();
		builder.build().forEach(group->{
			json.object();
			json.key("name");
			json.value(group.name);
			json.key("title");
			json.value(group.informal.title);
			json.endObject();
		});
		json.endArray();

	}
}
