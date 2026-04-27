package server.api.main;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.catalog.CatalogKey;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import server.api.APIHandler;
import util.JsonUtil;
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
		addItem(json, "RasterDB__WCS");
		addItem(json, "RasterDB__WMS");
		addItem(json, "VectorDB__WFS");
		addItem(json, "VectorDB__WMS");
		json.endArray();

		json.endObject();
	}

	private void addItem(JSONWriter json, String name) {
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

	private void handle_items(String name, Request request, Response response, UserIdentity userIdentity) throws IOException {
		switch(name) {
		case "RasterDB__WCS":
			handle_item_RasterDB__WCS(request, response, userIdentity);
			break;
		case "RasterDB__WMS":
			handle_item_RasterDB__WMS(request, response, userIdentity);
			break;
		case "VectorDB__WFS":
			handle_item_VectorDB__WFS(request, response, userIdentity);
			break;	
		case "VectorDB__WMS":
			handle_item_VectorDB__WMS(request, response, userIdentity);
			break;			
		default:
			throw new RuntimeException("unknown layer type: [" + name + "]");		
		}
	}
	
	private void handle_item_RasterDB__WCS(Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();

		json.key("stac_version").value("1.0.0");
		json.key("stac_extensions").array().endArray();
		json.key("type").value("Feature");
		json.key("id").value("RasterDB__WCS");

		json.key("geometry");
		json.object();
		json.endObject();

		json.key("properties");
		json.object();
		json.key("title").value("RasterDB WCS");
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


		for (String layerName : broker.getRasterdbNames()) {				
			RasterDB rasterdb = broker.getRasterdb(layerName);
			if (rasterdb.isAllowed(userIdentity)) {
				json.key(layerName + "__WCS");
				json.object();
				json.key("href").value("../../../rasterdb/" + layerName + "/wcs");
				json.key("type").value("application/xml");
				json.key("title").value(layerName + " - Web Coverage Service (WCS)");
				json.key("roles");
				json.array();
				json.value("data");
				json.endArray();
				json.endObject();
			}
		}

		json.endObject();

		json.endObject();
	}
	
	private void handle_item_RasterDB__WMS(Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();

		json.key("stac_version").value("1.0.0");
		json.key("stac_extensions").array().endArray();
		json.key("type").value("Feature");
		json.key("id").value("RasterDB__WMS");

		json.key("geometry");
		json.object();
		json.endObject();

		json.key("properties");
		json.object();
		json.key("title").value("RasterDB WMS");
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


		for (String layerName : broker.getRasterdbNames()) {				
			RasterDB rasterdb = broker.getRasterdb(layerName);
			if (rasterdb.isAllowed(userIdentity)) {
				json.key(layerName + "__WMS");
				json.object();
				json.key("href").value("../../../rasterdb/" + layerName + "/wms");
				json.key("type").value("application/xml");
				json.key("title").value(layerName + " - Web Map Service (WMS)");
				json.key("roles");
				json.array();
				json.value("data");
				json.endArray();
				json.endObject();
			}
		}

		json.endObject();

		json.endObject();
	}
	
	private void handle_item_VectorDB__WFS(Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();

		json.key("stac_version").value("1.0.0");
		json.key("stac_extensions").array().endArray();
		json.key("type").value("Feature");
		json.key("id").value("VectorDB__WFS");

		json.key("geometry");
		json.object();
		json.endObject();

		json.key("properties");
		json.object();
		json.key("title").value("VectorDB WFS");
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
		
		
		broker.catalog.getSorted(CatalogKey.TYPE_VECTORDB, userIdentity).forEach(entry -> {
			json.key(entry.name + "__WFS");
			json.object();
			json.key("href").value("../../../vectordbs/" + entry.name + "/wfs");
			json.key("type").value("application/xml");
			json.key("title").value(entry.name + " - Web Feature Service (WFS)");
			json.key("roles");
			json.array();
			json.value("data");
			json.endArray();
			json.endObject();
		});

		json.endObject();

		json.endObject();
	}
	
	private void handle_item_VectorDB__WMS(Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();

		json.key("stac_version").value("1.0.0");
		json.key("stac_extensions").array().endArray();
		json.key("type").value("Feature");
		json.key("id").value("VectorDB__WMS");

		json.key("geometry");
		json.object();
		json.endObject();

		json.key("properties");
		json.object();
		json.key("title").value("VectorDB WMS");
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
		
		
		broker.catalog.getSorted(CatalogKey.TYPE_VECTORDB, userIdentity).forEach(entry -> {
			json.key(entry.name + "__WMS");
			json.object();
			json.key("href").value("../../../vectordbs/" + entry.name + "/wms");
			json.key("type").value("application/xml");
			json.key("title").value(entry.name + " - Web Map Service (WMS)");
			json.key("roles");
			json.array();
			json.value("data");
			json.endArray();
			json.endObject();
		});

		json.endObject();

		json.endObject();
	}
}
