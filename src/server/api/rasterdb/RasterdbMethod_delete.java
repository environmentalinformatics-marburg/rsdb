package server.api.rasterdb;

import java.io.IOException;
import java.util.Iterator;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import rasterdb.RasterDB;
import util.Web;

public class RasterdbMethod_delete extends RasterdbMethod {
	

	public RasterdbMethod_delete(Broker broker) {
		super(broker, "delete");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info(request);
		request.setHandled(true);
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONObject action = json.getJSONObject("action");
			Iterator<String> it = action.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "delete_rasterdb": {
					rasterdb.checkMod(userIdentity);
					String rasterdb_name = action.getString("delete_rasterdb");
					if(!rasterdb_name.equals(rasterdb.config.getName())) {
						throw new RuntimeException("wrong parameters for delete rasterdb: " + rasterdb_name);
					}
					Logger.info("delete rasterdb " + rasterdb_name);
					broker.deleteRasterdb(rasterdb.config.getName());
					break;
				}				
				default: throw new RuntimeException("unknown key: "+key);
				}
			}
			response.setContentType(MIME_JSON);
			JSONWriter jsonResponse = new JSONWriter(response.getWriter());
			jsonResponse.object();
			jsonResponse.key("response");
			jsonResponse.object();
			jsonResponse.endObject();
			jsonResponse.endObject();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("reason");
			json.value(e.getMessage());
			json.endObject();
		} 	
	}
}