package server.api.roi_groups;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import broker.Broker;
import broker.group.RoiGroup;
import util.JsonUtil;
import util.Web;

public class APIHandler_roi_groups extends AbstractHandler {

	private final Broker broker;
	private final APIHandler_roi_group apihandler_roi_group;	

	public APIHandler_roi_groups(Broker broker) {
		this.broker = broker;
		apihandler_roi_group = new APIHandler_roi_group(broker);
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest r, HttpServletResponse response) throws IOException, ServletException {
		request.setHandled(true);
		UserIdentity userIdentity = Web.getUserIdentity(request);
		try {
			if(target.equals("/")) {
				handleList(request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in roi_groups: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				apihandler_roi_group.handle(name, next, request, (Response)response, userIdentity);
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
		json.key("roi_groups");
		json.array();
		for (RoiGroup group : broker.getRoiGroups()) {
			if(group.acl.isAllowed(userIdentity)) {
				json.object();
				json.key("name");
				json.value(group.name);
				json.key("title");
				json.value(group.informal.title);
				JsonUtil.writeOptList(json, "tags", group.informal.tags);
				json.endObject();
			}
		}
		json.endArray();
		json.endObject();
	}
}