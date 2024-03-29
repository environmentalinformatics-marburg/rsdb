package server.api.pointclouds;

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
import broker.Informal;
import util.JsonUtil;
import util.Web;

public class APIHandler_pointclouds extends AbstractHandler {

	private final Broker broker;
	private final APIHandler_pointcloud apihandler_pointcloud;	

	public APIHandler_pointclouds(Broker broker) {
		this.broker = broker;
		apihandler_pointcloud = new APIHandler_pointcloud(broker);
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
					throw new RuntimeException("no name in pointclouds: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				apihandler_pointcloud.handle(name, next, request, (Response)response, userIdentity);
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
		boolean withDescription = request.getParameter("description") != null;
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("pointclouds");
		json.array();
		for (String name : broker.getPointCloudNames()) {
			if(broker.isAllowedPointCloud(name, userIdentity)) {
				json.object();
				JsonUtil.put(json, "name", name);
				Informal informal = broker.getPointCloudInformal(name);
				JsonUtil.optPut(json, "title", informal.title);
				JsonUtil.writeOptList(json, "tags", informal.tags);
				if(withDescription) {
					JsonUtil.optPut(json, "description", informal.description);
				}
				json.endObject();
			}
		}
		json.endArray();
		json.endObject();
	}
}
