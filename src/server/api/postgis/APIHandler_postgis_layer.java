package server.api.postgis;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.Informal.Builder;
import broker.InformalProperties;
import broker.StructuredAccess;
import broker.acl.ACL;
import broker.acl.AclUtil;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.PostgisLayer;
import postgis.PostgisLayer.PostgisColumn;
import postgis.PostgisLayerManager;
import postgis.style.StyleProviderFactory;
import util.JsonUtil;
import util.Util;
import util.Web;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;

public class APIHandler_postgis_layer {

	private final Broker broker;
	private final PostgisLayerManager layerManager;
	private final PostgisHandler_wfs postgisHandler_wfs;
	private final PostgisHandler_wms postgisHandler_wms;
	private final PostgisHandler_indices postgisHandler_indices;
	private final PostgisHandler_image_png postgisHandler_image_png;
	private final PostgisHandler_table_csv postgisHandler_table_csv;

	public APIHandler_postgis_layer(Broker broker) {
		this.broker = broker;
		this.layerManager = broker.postgisLayerManager();
		this.postgisHandler_wfs = new PostgisHandler_wfs();
		this.postgisHandler_wms = new PostgisHandler_wms();
		this.postgisHandler_indices = new PostgisHandler_indices();
		this.postgisHandler_image_png = new PostgisHandler_image_png();
		this.postgisHandler_table_csv = new PostgisHandler_table_csv();
	}

	public void handle(String layerName, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			Util.checkStrictDotID(layerName);	
			if(target.equals("/")) {
				switch(request.getMethod()) {
				case "GET": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.check(userIdentity);
					handleGET(postgisLayer, request, response, userIdentity);
					break;
				}
				case "POST": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.checkMod(userIdentity);
					handlePOST(postgisLayer, request, response, userIdentity);
					break;
				}
				default:
					throw new RuntimeException("invalid HTTP method: " + request.getMethod());
				}
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
					postgisLayer.check(userIdentity);
					postgisHandler_wfs.handle(postgisLayer, next, request, response, userIdentity);
					break;
				}
				case "wms": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.check(userIdentity);
					postgisHandler_wms.handle(postgisLayer, next, request, response, userIdentity);
					break;
				}
				case "geometry.geojson": // alias for file download
				case "geojson": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.check(userIdentity);
					handleGeojson(postgisLayer, request, response, userIdentity);
					break;
				}
				case "indices": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.check(userIdentity);
					postgisHandler_indices.handle(postgisLayer, next, request, response, userIdentity);
					break;
				}
				case "image.png": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.check(userIdentity);
					postgisHandler_image_png.handle(postgisLayer, next, request, response, userIdentity);
					break;
				}
				case "table.csv": {
					PostgisLayer postgisLayer = layerManager.getPostgisLayer(layerName);
					postgisLayer.check(userIdentity);
					postgisHandler_table_csv.handle(postgisLayer, next, request, response, userIdentity);
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

	private void handleGET(PostgisLayer postgisLayer, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("name");
		json.value(postgisLayer.name);

		postgisLayer.informal().writeJson(json);

		json.key("modify");
		json.value(postgisLayer.isAllowedMod(userIdentity));
		json.key("owner");
		json.value(postgisLayer.isAllowedOwner(userIdentity));
		json.key("acl");
		postgisLayer.getACL().writeJSON(json);
		json.key("acl_mod");
		postgisLayer.getACL_mod().writeJSON(json);
		json.key("acl_owner");
		postgisLayer.getACL_owner().writeJSON(json);		

		json.key("epsg");
		json.value(postgisLayer.getEPSG());

		json.key("wkt_srs");
		json.value(postgisLayer.getWKT_SRS());

		Vec<String> vecInvalid = postgisLayer.isInvalid();
		if(vecInvalid != null) {
			json.key("invalid_geometry");
			json.value(vecInvalid);
		}

		Rect2d extent = postgisLayer.getExtent();
		if(extent != null) {
			json.key("extent");
			extent.toJSON(json);
		}

		int itemCount = postgisLayer.getItemCount();
		if(0 <= itemCount) {
			json.key("item_count");
			json.value(itemCount);
		}

		ReadonlyArray<String> geometryTypes = postgisLayer.getGeometryTypes();
		if(geometryTypes != null && !geometryTypes.isEmpty()) {
			json.key("geometry_types");
			json.array();
			for(String t : geometryTypes) {
				json.value(t);
			}
			json.endArray();
		}

		json.key("geometry_field");
		json.value(postgisLayer.primaryGeometryColumn);

		json.key("name_field");
		json.value(postgisLayer.getNameField());

		json.key("fields");
		json.array();
		//postgisLayer.fields.forEach(pc -> json.value(pc.name));
		postgisLayer.fields.stream().sorted(PostgisColumn.CASE_INSENSITIVE_ORDER).forEach(pc -> json.value(pc.name));
		json.endArray();

		json.key("class_fields");
		json.array();
		postgisLayer.getClass_fields().forEach(attr -> json.value(attr));
		json.endArray();

		if(postgisLayer.getStyleProvider() != StyleProviderFactory.DEFAULT_STYLE_PROVIDER) {
			json.key("style");
			postgisLayer.getStyleProvider().writeJson(json);
		}

		json.key("structured_access");		
		postgisLayer.getStructuredAccess().writeJSON(json);

		json.endObject();
	}

	private void handlePOST(PostgisLayer postgisLayer, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		boolean updateCatalog = false;
		boolean refreshPoiGroups = false;
		boolean refreshRoiGroups = false;
		try {
			JSONObject meta = new JSONObject(Web.requestContentToString(request));
			Iterator<String> it = meta.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "title": {
					postgisLayer.checkMod(userIdentity);
					updateCatalog = true;
					String title = meta.getString("title");
					Logger.info("set title " + title);
					Builder informal = postgisLayer.informal().toBuilder();
					informal.title = title.trim();
					postgisLayer.setInformal(informal.build());
					break;
				}
				case "description": {
					postgisLayer.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description");
					Logger.info("set description " + description);
					Builder informal = postgisLayer.informal().toBuilder();
					informal.description = description.trim();
					postgisLayer.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					postgisLayer.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact");
					Logger.info("set corresponding_contact " + corresponding_contact);
					Builder informal = postgisLayer.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact.trim();
					postgisLayer.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					postgisLayer.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date");
					Logger.info("set acquisition_date " + acquisition_date);
					Builder informal = postgisLayer.informal().toBuilder();
					informal.acquisition_date = acquisition_date.trim();
					postgisLayer.setInformal(informal.build());
					break;
				}				
				case "acl": {
					ACL acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl"));
					if(!postgisLayer.getACL().equals(acl)) {
						postgisLayer.checkOwner(userIdentity, "set voxeldb acl");
						updateCatalog = true;
						postgisLayer.setACL(acl);
					}
					break;
				}
				case "acl_mod": {
					ACL acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					if(!postgisLayer.getACL_mod().equals(acl_mod)) {
						postgisLayer.checkOwner(userIdentity, "set voxeldb acl_mod");	
						updateCatalog = true;
						postgisLayer.setACL_mod(acl_mod);
					}
					break;
				}
				case "acl_owner": {
					ACL acl_owner = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_owner"));
					if(!postgisLayer.getACL_owner().equals(acl_owner)) {
						AclUtil.check(userIdentity, "set voxeldb acl_owner");	
						updateCatalog = true;
						postgisLayer.setACL_owner(acl_owner);
					}
					break;
				}				
				case "tags": {
					postgisLayer.checkMod(userIdentity);
					updateCatalog = true;
					Builder informal = postgisLayer.informal().toBuilder();
					informal.setTags(JsonUtil.optStringTrimmedArray(meta, "tags"));
					postgisLayer.setInformal(informal.build());
					break;
				}				
				case "properties": {
					postgisLayer.checkMod(userIdentity);
					updateCatalog = true;	
					Builder informal = postgisLayer.informal().toBuilder();
					JSONObject jsonProperties = meta.getJSONObject("properties");
					InformalProperties.Builder properties = InformalProperties.Builder.ofJSON(jsonProperties);				
					informal.properties = properties;
					postgisLayer.setInformal(informal.build());
					break;
				}
				case "structured_access": {
					JSONObject st = meta.getJSONObject("structured_access");
					StructuredAccess structuredAccess = StructuredAccess.parseJSON(st);
					postgisLayer.setStructuredAccess(structuredAccess);
					updateCatalog = true;
					refreshPoiGroups = true;
					refreshRoiGroups = true;
					break;
				}				
				default: throw new RuntimeException("unknown key: "+key);
				}
			}
			response.setContentType(Web.MIME_JSON);
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
		} finally {
			if(updateCatalog) {
				broker.catalog.update(postgisLayer, false);
			}
			if(refreshPoiGroups) {
				broker.refreshPoiGroupMap();
			}
			if(refreshRoiGroups) {
				broker.refreshRoiGroupMap();
			}
		}
	}

	private void handleGeojson(PostgisLayer postgisLayer, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		

		String bboxParam = request.getParameter("bbox");
		Rect2d rect2d = null;
		if(bboxParam != null) {
			String[] bbox = bboxParam.split(",");
			rect2d = Rect2d.parseBbox(bbox);
		}		
		boolean crop = Web.getBoolean(request, "crop", true);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_GEO_JSON);
		//writeGeoJSON(postgisLayer, rect2d, crop, response.getWriter());	
		writeGeoJSONWithProperties(postgisLayer, rect2d, crop, response.getWriter());	
	}

	/**
	 * 
	 * @param postgisLayer
	 * @param rect2d  nullable
	 * @return
	 */
	public static void writeGeoJSON(PostgisLayer postgisLayer, Rect2d rect2d, boolean crop, PrintWriter out) {		
		int epsg = postgisLayer.getEPSG();
		GeojsonWriter geojsonWriter = new GeojsonWriter(out, epsg);
		geojsonWriter.acceptFields(null);
		geojsonWriter.acceptStart();
		postgisLayer.forEachGeoJSON(rect2d, crop, geometry -> {
			geojsonWriter.acceptFeatureStart(-1);
			geojsonWriter.acceptGeometry(geometry);
			geojsonWriter.acceptFeatureEnd(-1);
		});
		geojsonWriter.acceptEnd();
	}

	public static void writeGeoJSONWithProperties(PostgisLayer postgisLayer, Rect2d rect2d, boolean crop, PrintWriter out) {
		int epsg = postgisLayer.getEPSG();
		GeojsonWriter geojsonWriter = new GeojsonWriter(out, epsg);
		postgisLayer.forEachGeoJSONWithFields(rect2d, crop, geojsonWriter);		
	}
}

