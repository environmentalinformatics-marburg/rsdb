package server.api.main;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.PublicAccess;
import broker.PublicAccess.RasterDbAbstract;
import broker.PublicAccess.RasterDbWCS;
import broker.PublicAccess.RasterDbWMS;
import broker.acl.AclUtil;
import jakarta.servlet.http.HttpServletResponse;
import server.api.APIHandler;
import util.Web;

public class APIHandler_public_access extends APIHandler {
	

	public APIHandler_public_access(Broker broker) {
		super(broker, "public_access");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleGET(target, request, response);
			break;
		case "POST":
			handlePOST(target, request, response);
			break;
		default:
			throw new RuntimeException("invalid HTTP method: " + request.getMethod());
		}		
	}

	private void handleGET(String target, Request request, Response response) throws IOException {		
		AclUtil.check(Web.getUserIdentity(request), "get public URL list");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("public");
		json.object();

		broker.publicAccessManager().forEach((id, publicAccess) -> {
			json.key(id);
			json.object();
			json.key("type");
			json.value(publicAccess.type);
			switch(publicAccess.type) {
			case RasterDbWMS.TYPE:
			case RasterDbWCS.TYPE: {
				RasterDbAbstract a = (PublicAccess.RasterDbAbstract) publicAccess;
				json.key("rasterdb");
				json.value(a.rasterdb);
				break;
			}
			}
			json.endObject();
		});		

		json.endObject();
		json.endObject();		
	}

	private void handlePOST(String target, Request request, Response response) throws IOException {
		AclUtil.check(Web.getUserIdentity(request), "change public access URLs");
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONArray actions = json.getJSONArray("actions");
			int actionsLen = actions.length();
			for (int i = 0; i < actionsLen; i++) {
				JSONObject action = actions.getJSONObject(i);
				String key = action.getString("action");
				switch(key) {
				case "add_public_access": {
					PublicAccess publicAccess = null;
					String id = action.getString("id");
					String type = action.getString("type");
					switch(type) {
					case RasterDbWMS.TYPE: {
						String rasterdb = action.getString("rasterdb");
						publicAccess = new RasterDbWMS(id, rasterdb);
						break;
					}
					case RasterDbWCS.TYPE: {
						String rasterdb = action.getString("rasterdb");
						publicAccess = new RasterDbWCS(id, rasterdb);
						break;
					}					
					default:
						throw new RuntimeException("unknown type");
					}
					if(publicAccess != null) {
						broker.publicAccessManager().set(publicAccess);
					}
					break;
				}
				case "remove_public_access": {
					String id = action.getString("id");
					broker.publicAccessManager().remove(id);
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
			response.getWriter().append(e.toString());
		}
	}
}
