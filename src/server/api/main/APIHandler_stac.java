package server.api.main;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import server.api.APIHandler;
import util.Web;

public class APIHandler_stac extends APIHandler {


	public APIHandler_stac(Broker broker) {
		super(broker, "stac");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Logger.info(target);
		UserIdentity userIdentity = Web.getUserIdentity(request);

		try {
			if(target.isEmpty() || target.equals("/")) {
				handle_root(request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in voxeldbs: "+target);
				}			
				String name = i < 0 ? target.substring(0) : target.substring(0, i);
				String next = i < 0 ? "/" : target.substring(i + 1);
				switch(name) {
				case "items": {
					handle_items(next, request, response, userIdentity);
					break;
				}
				default: {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					response.setContentType(Web.MIME_TEXT);
					response.getWriter().println(name);
				}
				}

			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e);
		}



	}

	private void handle_root(Request request, Response response, UserIdentity userIdentity) throws IOException {
		
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String domain = serverName + ":" + serverPort;
		
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();

		json.key("id");
		json.value("simple-collection");
		json.key("type");
		json.value("Collection");
		json.key("stac_extensions");
		json.array();
		json.endArray();
		json.key("stac_version");
		json.value("1.0.0");
		json.key("description");
		json.value("RSDB layer data access endpoints");
		json.key("title");
		json.value("RSDB layers");

		json.key("providers");
		json.array();
		json.object();
		json.key("name");
		json.value("RSDB");
		json.key("description");
		json.value("RSDB server");
		json.key("roles");
		json.array();
		json.value("producer");
		json.value("processor");
		json.endArray();
		json.key("url");
		json.value(domain);
		json.endObject();
		json.endArray();

		json.key("extent");
		json.object();
		json.key("spatial");
		json.object();
		json.key("bbox");
		json.value(null);
		json.endObject();
		json.key("temporal");
		json.object();
		json.key("interval");
		json.value(null);
		json.endObject();
		json.endObject();

		json.key("license");
		json.value("Data policy");

		json.key("links");
		json.array();
		for (String name : broker.getRasterdbNames()) {				
			RasterDB rasterdb = broker.getRasterdb(name);
			if (rasterdb.isAllowed(userIdentity)) {
				json.object();
				json.key("rel");
				json.value("item");
				json.key("href");
				json.value("./stac/items/" + name);
				json.key("type");
				json.value("application/geo+json");
				json.key("title");
				json.value(name);
				json.endObject();

			}
		}
		json.endArray();

		json.endObject();
	}

	private void handle_items(String name, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();

		json.key("stac_version").value("1.0.0");
		json.key("stac_extensions").array().endArray();
		json.key("type").value("Feature");
		json.key("id").value(name);

		json.key("geometry");
		json.object();
		json.endObject();

		json.key("properties");
		json.object();
		json.key("title").value(name);
		json.key("description").value("RSDB layer");
		json.key("datetime").value(null);
		json.key("start_datetime").value("1970-01-01T00:00:00Z");
		json.key("end_datetime").value("1970-01-01T00:00:00Z");
		json.endObject();

		json.key("collection").value("simple-collection");

		json.key("links");
		json.array();
		json.endArray();

		json.key("assets");
		json.object();
		
		/*for (int i = 0; i < 30; i++) {
			json.key("WCS" + i);
			json.object();
			json.key("href").value("../../../rasterdb/" + name + "/wcs");
			json.key("type").value("application/xml");
			json.key("title").value(name + " - Web Coverage Service (WCS)");
			json.key("roles");
			json.array();
			json.value("data");
			json.endArray();
			json.endObject();
		}*/
		
		json.key("WCS");
		json.object();
		json.key("href").value("../../../rasterdb/" + name + "/wcs");
		json.key("type").value("application/xml");
		json.key("title").value(name + " - Web Coverage Service (WCS)");
		json.key("roles");
		json.array();
		json.value("data");
		json.endArray();
		json.endObject();
		
		json.endObject();

		json.endObject();
	}
}
