package server.api.postgis;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.PostgisLayer;
import broker.PostgisLayer.PostgisColumn;
import broker.PostgisLayer.FeatureConsumer;
import broker.PostgisLayerManager;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import util.Util;
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
			Util.checkStrictDotID(layerName);	
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
		
		String bboxParam = request.getParameter("bbox");
		Rect2d rect2d = null;
		if(bboxParam != null) {
			String[] bbox = bboxParam.split(",");
			rect2d = Rect2d.parseBbox(bbox);
		}
		
		//String geojson = getGeoJSON(postgisLayer, rect2d);
		String geojson = getGeoJSONWithProperties(postgisLayer, rect2d);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/geo+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().print(geojson);		
	}

	/**
	 * 
	 * @param postgisLayer
	 * @param rect2d  nullable
	 * @return
	 */
	private String getGeoJSON(PostgisLayer postgisLayer, Rect2d rect2d) {

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

		postgisLayer.forEachGeoJSON(rect2d, new Consumer<String>() {			
			boolean isFirst = true;
			StringBuilder sb1 = sb;			
			@Override
			public void accept(String geometry) {
				if(isFirst) {
					isFirst = false;
				} else {
					sb1.append(",");						
				}
				sb1.append("{\"type\":\"Feature\",\"geometry\":");
				sb1.append(geometry);				
				sb1.append("}");
			}
		});

		sb.append("]");
		sb.append("}");

		return sb.toString();
	}
	
	private String getGeoJSONWithProperties(PostgisLayer postgisLayer, Rect2d rect2d) {

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

		postgisLayer.forEachGeoJSONWithProperties(rect2d, new FeatureConsumer() {			
			StringBuilder sb1 = sb;			

			@Override
			public void acceptFeatureStart(boolean isFirstFeature) {
				if(isFirstFeature) {
					sb1.append("{\"type\":\"Feature\"");
				} else {
					sb1.append(",{\"type\":\"Feature\"");						
				}			
			}
			@Override
			public void acceptFeatureGeometry(String geometry) {
				sb1.append(",\"geometry\":");
				sb1.append(geometry);				
			}
			@Override
			public void acceptFeatureFieldsStart() {
				sb1.append(",\"properties\":{");
			}
			@Override
			public void acceptFeatureField(PostgisColumn field, String fieldValue, boolean isFirstFeatureField) {	
				if(isFirstFeatureField) {
					sb1.append("\"" + field.name + "\":\"" + fieldValue + "\"");
				} else {
					sb1.append(",\"" + field.name + "\":\"" + fieldValue + "\"");						
				}
			}
			@Override
			public void acceptFeatureFieldNull(PostgisColumn field, boolean isFirstFeatureField) {
				if(isFirstFeatureField) {
					sb1.append("\"" + field.name + "\":null");
				} else {
					sb1.append(",\"" + field.name + "\":null");						
				}
			}
			@Override
			public void acceptFeatureFieldInt32(PostgisColumn field, int fieldValue, boolean isFirstFeatureField) {
				if(isFirstFeatureField) {
					sb1.append("\"" + field.name + "\":" + fieldValue);
				} else {
					sb1.append(",\"" + field.name + "\":" + fieldValue);						
				}
			}
			@Override
			public void acceptFeatureFieldsEnd() {
				sb1.append("}");				
			}
			@Override
			public void acceptFeatureEnd() {
				sb1.append("}");				
			}						
		});

		sb.append("]");
		sb.append("}");

		return sb.toString();
	}
}

