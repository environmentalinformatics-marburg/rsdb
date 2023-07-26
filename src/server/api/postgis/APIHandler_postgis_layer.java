package server.api.postgis;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.PostgisLayer;
import broker.PostgisLayerManager;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class APIHandler_postgis_layer {

	private final Broker broker;
	private final PostgisLayerManager layerManager;
	private final PostgisHandler_wfs postgisHandler_wfs;
	private final PostgisHandler_indices postgisHandler_indices;

	public APIHandler_postgis_layer(Broker broker) {
		this.broker = broker;
		this.layerManager = broker.postgisLayerManager();
		this.postgisHandler_wfs = new PostgisHandler_wfs();
		this.postgisHandler_indices = new PostgisHandler_indices();
	}

	public void handle(String layerName, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			//Util.checkStrictID(layerName);	
			if(target.equals("/")) {
				PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
				handleList(postgisLayer, request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(name) {
				case "wfs": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisHandler_wfs.handle(postgisLayer, next, request, response, userIdentity);
					break;
				}
				case "geojson": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					handleGeojson(postgisLayer, request, response, userIdentity);
					break;
				}
				case "indices": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisHandler_indices.handle(postgisLayer, next, request, response, userIdentity);
					break;
				}
				default:
					throw new RuntimeException("unknown target: ");
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

	private void handleList(PostgisLayer postgisLayer, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("name");
		json.value(postgisLayer.name);
		json.key("epsg");
		json.value(postgisLayer.getEPSG());
		json.endObject();
	}

	private void handleGeojson(PostgisLayer postgisLayer, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		
		String geojson = getGeoJSON(postgisLayer);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/geo+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().print(geojson);		
	}

	private String getGeoJSON(PostgisLayer postgisLayer) {

		int epsg = postgisLayer.getEPSG();

		StringBuilder sb = new StringBuilder();
		sb.append("{\"type\":\"FeatureCollection\"");
		if(epsg > 0) {
			sb.append(",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"urn:ogc:def:crs:EPSG::");
			sb.append(epsg);
			sb.append("\"}}");
		}
		sb.append(",\"features\":");
		sb.append("[");

		boolean[] isFirst = new boolean[] {true};

		postgisLayer.forEachGeoJSON(geometry -> {
			if(isFirst[0]) {
				isFirst[0] = false;
			} else {
				sb.append(",");						
			}
			sb.append("{\"type\":\"Feature\",\"geometry\":");
			sb.append(geometry);
			sb.append("}");
		});

		sb.append("]");
		sb.append("}");

		return sb.toString();
	}
}

