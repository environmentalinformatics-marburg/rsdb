package server.api;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import util.Web;

public class APIHandler_records extends APIHandler {

	public APIHandler_records(Broker broker) {
		super(broker, "records");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Logger.info("handle: " + target);
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
				case "conformance": {
					handle_conformance(next, request, response, userIdentity);
					break;
				}
				case "collections": {
					handle_collections(next, request, response, userIdentity);
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
		String domain = request.getServerName() + ":" + request.getServerPort();

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();
		json.key("title");
		json.value("RSDB Records API");
		json.key("description");
		json.value("OGC API Records - Geodaten Metadaten");
		json.key("links");
		json.array();

		json.object();
		json.key("rel");
		json.value("self");
		json.key("type");
		json.value("application/json");
		json.key("href");
		json.value("http://" + domain + "/api/records");
		json.endObject();

		json.object();
		json.key("rel");
		json.value("conformance");
		json.key("type");
		json.value("application/json");
		json.key("href");
		json.value("http://" + domain + "/api/records/conformance");
		json.endObject();

		json.object();
		json.key("rel");
		json.value("collections");
		json.key("type");
		json.value("application/json");
		json.key("href");
		json.value("http://" + domain + "/api/records/collections");
		json.endObject();

		json.endArray();
		json.endObject();
	}

	private void handle_conformance(String name, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();
		json.key("conformsTo");
		json.array();
		json.value("http://www.opengis.net/spec/ogcapi-records-1/1.0/conf/core");
		json.value("http://www.opengis.net/spec/ogcapi-records-1/1.0/conf/sorting");
		json.endArray();
		json.endObject();
	}

	private void handle_collections(String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info("handle_collections: " + target);
		try {
			if(target.isEmpty() || target.equals("/")) {
				handle_collections_root(request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in: "+target);
				}			
				String name = i < 0 ? target.substring(0) : target.substring(0, i);
				String next = i < 0 ? "/" : target.substring(i + 1);
				Logger.info("collection: " + name + " content  "  + next);
				int i2 = next.indexOf('/', 1);
				String name2 = i2 < 0 ? next.substring(0) : next.substring(0, i2);
				String next2 = i2 < 0 ? "/" : next.substring(i2 + 1);
				Logger.info("collection: " + name + " content1: "  + name2 + " content2: " + next2);
				switch(name2) {
				case "items": {					
					handle_collections_items(name, next2, request, response, userIdentity);
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

	private void handle_collections_root(Request request, Response response, UserIdentity userIdentity) throws IOException {
		String domain = request.getServerName() + ":" + request.getServerPort();

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();
		json.key("collections");
		json.array();

		json.object();
		json.key("id");
		json.value("datasets");
		json.key("title");
		json.value("Geodaten Datensätze");
		json.key("description");
		json.value("Metadaten zu verfügbaren Geodaten");
		json.key("itemType");
		json.value("record");
		json.key("links");
		json.array();

		json.object();
		json.key("rel");
		json.value("self");
		json.key("href");
		json.value("http://" + domain + "/api/records/collections/datasets");
		json.endObject();

		json.object();
		json.key("rel");
		json.value("items");
		json.key("href");
		json.value("http://" + domain + "/api/records/collections/datasets/items");
		json.endObject();

		json.endArray();
		json.endObject();

		json.endArray();

		json.key("links");
		json.array();

		json.object();
		json.key("rel");
		json.value("self");
		json.key("href");
		json.value("http://" + domain + "/api/records/collections");
		json.endObject();

		json.endArray();
		json.endObject();
	}

	private void handle_collections_items(String collection_name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info("handle_collections_items: " + collection_name + " target: " +target);
		try {
			if(target.isEmpty() || target.equals("/")) {
				handle_collections_items_root(collection_name, request, response, userIdentity);
			} else {
				handle_collections_items_item(collection_name, target, request, response, userIdentity);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e);
		}
	}

	private void handle_collections_items_root(String collection_name, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info(collection_name);
		
		String domain = request.getServerName() + ":" + request.getServerPort();

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();
		json.key("type");
		json.value("FeatureCollection");

		json.key("features");
		json.array();

		for(String layerName : broker.getRasterdbNames()) {
			RasterDB rasterdb = broker.getRasterdb(layerName);
			if(rasterdb.isAllowed(userIdentity)) {
				json.object();
				json.key("id");
				json.value(layerName + "__WMS");
				json.key("type");
				json.value("Feature");

				json.key("geometry");
				json.object();
				json.key("type");
				json.value("Polygon");
				json.key("coordinates");
				json.array();              // coordinates array
				json.array();              // outer ring
				json.array(); json.value(5); json.value(47); json.endArray();
				json.array(); json.value(15); json.value(47); json.endArray();
				json.array(); json.value(15); json.value(55); json.endArray();
				json.array(); json.value(5); json.value(55); json.endArray();
				json.array(); json.value(5); json.value(47); json.endArray();
				json.endArray();           // end outer ring
				json.endArray();           // end coordinates
				json.endObject();          // end geometry

				json.key("properties");
				json.object();
				json.key("identifier");
				json.value(layerName + "__WMS");
				json.key("title");
				json.value(layerName + " WMS");
				json.key("description");
				json.value("RSDB layer");
				json.key("type");
				json.value("http://www.opengis.net/def/metadata/2.0/schema/iso/19115/-3/mrl/1.0");

				json.key("links");
				json.array();

				json.object();
				json.key("rel");
				json.value("service");
				json.key("type");
				json.value("application/xml");
				json.key("url");
				json.value("https://" + domain + "/rasterdb/" + layerName + "/wms");
				json.key("layer");
				json.value(layerName);
				json.endObject();

				json.object();
				json.key("rel");
				json.value("self");
				json.key("type");
				json.value("application/geo+json");
				json.key("href");
				json.value("https://" + domain + "/records/collections/datasets/items/" + layerName + "__WMS");
				json.endObject();

				json.endArray();
				json.endObject();
				json.endObject();
			}
		}

		json.endArray();

		json.key("links");
		json.array();

		json.object();
		json.key("rel");
		json.value("self");
		json.key("href");
		json.value("https://" + domain + "/records/collections/" + collection_name + "/items");
		json.endObject();

		json.object();
		json.key("rel");
		json.value("collection");
		json.key("href");
		json.value("https://" + domain + "/records/collections/" + collection_name);
		json.endObject();

		json.endArray();

		json.key("numberMatched");
		json.value(broker.getRasterdbNames().size());
		json.key("numberReturned");
		json.value(broker.getRasterdbNames().size());

		json.endObject();
	}

	private void handle_collections_items_item(String collection_name, String itemId, Request request, Response response, UserIdentity userIdentity) throws IOException {
	    Logger.info(collection_name);
	    
	    if(itemId.endsWith("__WMS")) {
	    	String rasterdbId = itemId.substring(0, itemId.length() - "__WMS".length());
	    	handle_collections_items_rasterdb(collection_name, itemId, rasterdbId, request, response, userIdentity);
	    }

	}

	private void handle_collections_items_rasterdb(String collection_name, String itemId, String rasterdbId, Request request,
			Response response, UserIdentity userIdentity) throws IOException {
Logger.info(collection_name);
	    
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String baseURL = scheme + "://" + serverName + ":" + serverPort;

	    String domain = request.getServerName() + ":" + request.getServerPort();

	    response.setStatus(HttpServletResponse.SC_OK);
	    response.setContentType(Web.MIME_JSON);
	    JSONWriter json = new JSONWriter(response.getWriter());

	    json.object(); // Start Feature object

	    json.key("id");
	    json.value(itemId);
	    json.key("type");
	    json.value("Feature");

	    json.key("geometry");
	    json.object();
	    json.key("type");
	    json.value("Polygon");
	    json.key("coordinates");
	    json.array();
	    json.array().value(5).value(47).endArray();
	    json.array().value(15).value(47).endArray();
	    json.array().value(15).value(55).endArray();
	    json.array().value(5).value(55).endArray();
	    json.array().value(5).value(47).endArray();
	    json.endArray();
	    json.endObject();

	    json.key("properties");
	    json.object();
	    json.key("identifier");
	    json.value(itemId);
	    json.key("title");
	    json.value(itemId.replace("__", " "));
	    json.key("description");
	    json.value("RSDB layer");
	    json.key("type");
	    //json.value("http://www.opengis.net/def/metadata/2.0/schema/iso/19115/-3/mrl/1.0");
	    //json.value("https://" + domain + "/rasterdb/" + itemId.replace("__WMS", "") + "/wms");
	    json.value(baseURL + "/rasterdb/" + rasterdbId + "/wms");
	    json.endObject();

	    json.key("links");
	    json.array();

	    // First link: WMS service
	    json.object();
	    json.key("rel");
	    json.value("service");
	    json.key("type");
	    json.value("application/xml");
	    json.key("url");
	    json.value("https://" + domain + "/rasterdb/" + itemId.replace("__WMS", "") + "/wms");
	    json.key("layer");
	    json.value(itemId);
	    json.endObject();

	    // Second link: self
	    json.object();
	    json.key("rel");
	    json.value("self");
	    json.key("type");
	    json.value("application/geo+json");
	    json.key("href");
	    json.value("https://" + domain + "/records/collections/"+ collection_name +"/items/" + itemId);
	    json.endObject();

	    // Third link: collection
	    json.object();
	    json.key("rel");
	    json.value("collection");
	    json.key("href");
	    json.value("https://" + domain + "/records/collections/"+ collection_name);
	    json.endObject();

	    json.endArray();

	    json.endObject(); // Close Feature object
		
	}
}