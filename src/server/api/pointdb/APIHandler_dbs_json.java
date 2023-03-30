package server.api.pointdb;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import pointdb.base.PointdbConfig;
import util.JsonUtil;
import util.Web;

public class APIHandler_dbs_json extends PointdbAPIHandler {

	public APIHandler_dbs_json(Broker broker) {
		super(broker, "dbs.json");		
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		if(request.getParameter("structured") == null) {
			json.array();
			for(PointdbConfig config:broker.brokerConfig.pointdbMap().values()) {
				if(config.isAllowed(Web.getUserIdentity(request))) {
					json.value(config.name);
				}
			}
			json.endArray();		
		} else {
			json.object();
			json.key("pointdbs");
			json.array();			
			for(PointdbConfig config:broker.brokerConfig.pointdbMap().values()) {
				if(config.isAllowed(Web.getUserIdentity(request))) {
					json.object();
					JsonUtil.put(json, "name", config.name);
					JsonUtil.put(json, "description", config.informal().description);
					json.endObject();
				}
			}
			json.endArray();
			json.endObject();
		}

	}
}
