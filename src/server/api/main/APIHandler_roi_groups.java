package server.api.main;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.RoiGroup;
import pointdb.PointDB;
import server.api.APIHandler;
import util.Web;

public class APIHandler_roi_groups extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_roi_groups(Broker broker) {
		super(broker, "roi_groups");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		UserIdentity userIdentity = Web.getUserIdentity(request);

		Builder<RoiGroup> builder = Stream.<RoiGroup>builder();

		String pointdbName = request.getParameter("pointdb");

		if(pointdbName!=null) {			
			PointDB db = broker.getPointdb(pointdbName);
			db.config.check(userIdentity);
			db.config.getRoiGroupNames().forEach(roiGroupName-> {
				try {
					RoiGroup roiGroup = broker.getRoiGroup(roiGroupName);
					if(roiGroup.acl.isAllowed(userIdentity)) {
						builder.accept(roiGroup);
					}
				} catch(Exception e) {
					log.warn(e);
				}
			});
		} else {
			broker.getRoiGroups().forEach(roiGroup->{
				if(roiGroup.acl.isAllowed(userIdentity)) {
					builder.accept(roiGroup);
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
