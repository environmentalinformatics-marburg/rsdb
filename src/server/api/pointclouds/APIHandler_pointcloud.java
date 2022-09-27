package server.api.pointclouds;

import java.io.IOException;
import java.util.Iterator;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.proj.Projection;
import org.locationtech.proj4j.units.Unit;

import broker.Broker;
import broker.InformalProperties;
import broker.TimeSlice;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import broker.group.PoiGroup;
import broker.group.RoiGroup;
import pointcloud.DoublePoint;
import pointcloud.DoubleRect;
import pointcloud.PointCloud;
import util.JsonUtil;
import util.Range2d;
import util.Web;

public class APIHandler_pointcloud {


	protected static final String MIME_JSON = "application/json";

	private final Broker broker;
	private final APIHandler_points apihandler_points;	
	private final APIHandler_raster apihandler_raster;	
	private final APIHandler_volume apihandler_volume;
	private final APIHandler_indices apihandler_indices;
	private final APIHandler_index_list apihandler_index_list;

	public APIHandler_pointcloud(Broker broker) {
		this.broker = broker;
		this.apihandler_points = new APIHandler_points(broker);
		this.apihandler_raster = new APIHandler_raster(broker);
		this.apihandler_volume = new APIHandler_volume(broker);
		this.apihandler_indices = new APIHandler_indices(broker);
		this.apihandler_index_list = new APIHandler_index_list(broker);
	}

	public void handle(String name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info("get: " + name);
		PointCloud pointcloud = broker.getPointCloud(name);
		Logger.info("get: " + pointcloud);
		if(pointcloud == null) {
			throw new RuntimeException("PointCloud not found: " + name);
			/*Logger.error("PointCloud not found: " + name);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("PointCloud not found: " + name);*/
		} else if(target.equals("/")) {
			switch(request.getMethod()) {
			case "GET":
				pointcloud.check(userIdentity);
				handleGET(pointcloud, request, response, userIdentity);
				break;
			case "POST":
				pointcloud.checkMod(userIdentity);
				handlePOST(pointcloud, request, response, userIdentity);
				break;
			default:
				throw new RuntimeException("invalid HTTP method: " + request.getMethod());
			}
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name in pointclouds: " + target);
			}			
			String resource = i < 0 ? target.substring(1) : target.substring(1, i);
			int formatIndex = resource.lastIndexOf('.');
			String resourceName = formatIndex < 0 ? resource : resource.substring(0, formatIndex);
			String resourceFormat = formatIndex < 0 ? "" : resource.substring(formatIndex + 1);
			Logger.info("resourceName: "+resourceName);
			Logger.info("resourceFormat: "+resourceFormat);
			String next = i < 0 ? "/" : target.substring(i);
			if(next.equals("/")) {
				switch(resourceName) {
				case "points":
					pointcloud.check(userIdentity);
					apihandler_points.handle(pointcloud, resourceFormat, request, response);
					break;
				case "raster":
					pointcloud.check(userIdentity);
					apihandler_raster.handle(pointcloud, resourceFormat, request, response);
					break;
				case "volume":
					pointcloud.check(userIdentity);
					apihandler_volume.handle(pointcloud, resourceFormat, request, response);
					break;
				case "indices":
					pointcloud.check(userIdentity);
					apihandler_indices.handle(pointcloud, resourceFormat, request, response);
					break;
				case "index_list":
					pointcloud.check(userIdentity);
					apihandler_index_list.handle(pointcloud, resourceFormat, request, response);
					break;					
				default:
					throw new RuntimeException("unknown resource: " + resource);
				}
			} else {
				throw new RuntimeException("error in subpath: " + target);
			}
		}		
	}

	private void handleGET(PointCloud pointcloud, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		
		boolean requestExtent = request.getParameter("extent") != null;
		DoubleRect range = requestExtent ? pointcloud.getRange() : null;
		Range2d cell_range = requestExtent ? pointcloud.getCellRange() : null;

		boolean requestPoiGroups = request.getParameter("poi_groups") != null;
		boolean requestRoiGroups = request.getParameter("roi_groups") != null;
		boolean requestStorageSize = request.getParameter("storage_size") != null;
		boolean requestInternalStorageInternalFreeSize = request.getParameter("storage_internal_free_size") != null;
		boolean requestCellCount = request.getParameter("cell_count") != null;
		boolean requestCellSizeStats = request.getParameter("cell_size_stats") != null;


		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("pointcloud");
		json.object();
		json.key("name");
		json.value(pointcloud.config.name);
		pointcloud.informal().writeJson(json);
		json.key("code");
		json.value(pointcloud.getCode());
		json.key("proj4");
		json.value(pointcloud.getProj4());



		Unit unit = null;
		if(pointcloud.hasProj4()) {
			try {
				CoordinateReferenceSystem crs = new CRSFactory().createFromParameters(null, pointcloud.getProj4());
				if(crs != null) {
					Projection projection = crs.getProjection();
					if(projection != null) {
						unit = projection.getUnits();
					}
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
		}
		if(unit == null && pointcloud.hasCode()) {
			try {
				CoordinateReferenceSystem crs = new CRSFactory().createFromName(pointcloud.getCode());
				if(crs != null) {
					Projection projection = crs.getProjection();
					if(projection != null) {
						unit = projection.getUnits();
					}
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
		}
		if(unit != null) {
			json.key("projection_unit");
			json.object();
			json.key("name");
			json.value(unit.name);
			json.endObject();
		}
		json.key("cell_size");
		json.value(pointcloud.getCellsize());
		json.key("cell_scale");
		json.value(pointcloud.getCellscale());
		if(requestExtent) {
			if(range != null) {
				json.key("extent");
				json.array();
				json.value(range.xmin);
				json.value(range.ymin);
				json.value(range.xmax);
				json.value(range.ymax);
				json.endArray();
			}
			if(cell_range != null) {
				json.key("cell_extent");
				json.array();
				json.value(cell_range.xmin);
				json.value(cell_range.ymin);
				json.value(cell_range.xmax);
				json.value(cell_range.ymax);
				json.endArray();
			}
			if(cell_range != null) {
				json.key("cell_range");
				json.object();
				json.key("x");
				json.value(cell_range.xmax - cell_range.xmin + 1);
				json.key("y");
				json.value(cell_range.ymax - cell_range.ymin + 1);
				json.endObject();
			}
			if(cell_range != null) {
				json.key("range");
				json.object();
				json.key("x");
				json.value((cell_range.xmax - cell_range.xmin + 1) * pointcloud.getCellsize());
				json.key("y");
				json.value((cell_range.ymax - cell_range.ymin + 1) * pointcloud.getCellsize());
				json.endObject();
			}
			DoublePoint cell_offset = pointcloud.getCelloffset();
			if(cell_offset != null) {
				json.key("cell_offset");
				json.object();
				json.key("x");
				json.value(cell_offset.x * pointcloud.getCellsize());
				json.key("y");
				json.value(cell_offset.y * pointcloud.getCellsize());
				json.endObject();
			}
		}
		json.key("raster_types");
		json.array();
		for(String[] rasterType:APIHandler_raster.RASTER_TYPES) {
			json.object();
			json.key("name");
			json.value(rasterType[0]);
			json.key("description");
			json.value(rasterType[1]);
			json.endObject();
		}
		json.endArray();
		json.key("modify");
		json.value(pointcloud.isAllowedMod(userIdentity));
		json.key("owner");
		json.value(pointcloud.isAllowedOwner(userIdentity));
		json.key("acl");
		pointcloud.getACL().writeJSON(json);
		json.key("acl_mod");		
		pointcloud.getACL_mod().writeJSON(json);
		json.key("acl_owner");
		pointcloud.getACL_owner().writeJSON(json);
		json.key("attributes");
		json.value(pointcloud.getSelector().toArray());
		json.key("associated");
		pointcloud.getAssociated().writeJSON(json);
		if(requestPoiGroups) {
			json.key("poi_groups");
			json.array();
			for(String name:pointcloud.getAssociated().getPoiGroups()) {
				try {
					PoiGroup poiGroup = broker.getPoiGroup(name);
					json.object();
					json.key("name");
					json.value(poiGroup.name);
					json.key("title");
					json.value(poiGroup.informal.title);
					json.endObject();
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			json.endArray();
		}
		if(requestRoiGroups) {
			json.key("roi_groups");
			json.array();
			for(String name:pointcloud.getAssociated().getRoiGroups()) {
				try {
					RoiGroup roiGroup = broker.getRoiGroup(name);
					json.object();
					json.key("name");
					json.value(roiGroup.name);
					json.key("title");
					json.value(roiGroup.informal.title);
					json.endObject();
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			json.endArray();
		}
		if(requestStorageSize) {
			try {
				long storage_size = pointcloud.getGriddb().storage().calculateStorageSize();
				json.key("storage_size");
				json.value(storage_size);
			} catch(Exception e) {
				Logger.warn(e);
			}
		}
		if(requestInternalStorageInternalFreeSize) {
			try {
				long internal_free_size = pointcloud.getGriddb().storage().calculateInternalFreeSize();
				json.key("storage_internal_free_size");
				json.value(internal_free_size);
			} catch(Exception e) {
				Logger.warn(e);
			}
		}
		if(requestCellCount) {
			json.key("cell_count");
			int cell_count = pointcloud.getGriddb().storage().calculateTileCount();
			json.value(cell_count);
		}
		if(requestCellSizeStats) {
			long[] stats = pointcloud.getGriddb().storage().calculateTileSizeStats();
			if(stats != null) {
				json.key("cell_size_stats");
				json.object();
				json.key("min");
				json.value(stats[0]);		
				json.key("mean");
				json.value(stats[1]);		
				json.key("max");
				json.value(stats[2]);		
				json.endObject();		
			}
		}
		json.key("time_slices");
		TimeSlice.timeSlicesToJSON(pointcloud.timeMapReadonly.values(), json);		
		json.endObject(); // pointcloud
		json.endObject(); // JSON
	}

	private void handlePOST(PointCloud pointcloud, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		
		boolean updateCatalog = false;
		boolean updateCatalogPoints = false;
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONObject meta = json.getJSONObject("pointcloud");
			Iterator<String> it = meta.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "title": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String title = meta.getString("title");
					Logger.info("set title " + title);
					Builder informal = pointcloud.informal().toBuilder();
					informal.title = title.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "description": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description");
					Logger.info("set description " + description);
					Builder informal = pointcloud.informal().toBuilder();
					informal.description = description.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact");
					Logger.info("set corresponding_contact " + corresponding_contact);
					Builder informal = pointcloud.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date");
					Logger.info("set acquisition_date " + acquisition_date);
					Builder informal = pointcloud.informal().toBuilder();
					informal.acquisition_date = acquisition_date.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "code": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String code = meta.getString("code").trim();
					Logger.info("set code "+code);
					pointcloud.setCode(code);
					break;
				}
				case "proj4": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					updateCatalogPoints = true;
					String proj4 = meta.getString("proj4").trim();
					Logger.info("set proj4 "+proj4);
					pointcloud.setProj4(proj4);
					break;
				}
				case "acl": {
					ACL acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl"));
					if(!pointcloud.getACL().equals(acl)) {
						pointcloud.checkOwner(userIdentity, "set pointcloud acl");
						updateCatalog = true;
						pointcloud.setACL(acl);
					}
					break;
				}
				case "acl_mod": {
					ACL acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					if(!pointcloud.getACL_mod().equals(acl_mod)) {
						pointcloud.checkOwner(userIdentity, "set pointcloud acl_mod");
						updateCatalog = true;
						pointcloud.setACL_mod(acl_mod);
					}
					break;
				}
				case "acl_owner": {
					ACL acl_owner = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_owner"));
					if(!pointcloud.getACL_owner().equals(acl_owner)) {
						AclUtil.check(userIdentity, "set pointcloud acl_owner");
						updateCatalog = true;							
						pointcloud.setACL_owner(acl_owner);
					}
					break;
				}
				case "tags": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					Builder informal = pointcloud.informal().toBuilder();
					informal.setTags(JsonUtil.optStringTrimmedArray(meta, "tags"));
					pointcloud.setInformal(informal.build());
					break;
				}
				case "associated": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					JSONObject jsonAssociated = meta.getJSONObject("associated");
					pointcloud.setAssociatedRasterDB(jsonAssociated.optString("rasterdb", ""));
					pointcloud.setAssociatedPoiGroups(JsonUtil.optStringTrimmedList(jsonAssociated, "poi_groups"));
					pointcloud.setAssociatedRoiGroups(JsonUtil.optStringTrimmedList(jsonAssociated, "roi_groups"));
					break;
				}
				case "delete_pointcloud": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = false; // will be updated by delete pointcloud
					String pointcloud_name = meta.getString("delete_pointcloud");
					if(!pointcloud_name.equals(pointcloud.getName())) {
						throw new RuntimeException("wrong parameters for delete pointcloud: " + pointcloud_name);
					}
					Logger.info("delete pointcloud " + pointcloud_name);
					broker.deletePointCloud(pointcloud.getName());
					pointcloud = null;
					break;
				}
				case "properties": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;	
					Builder informal = pointcloud.informal().toBuilder();
					JSONObject jsonProperties = meta.getJSONObject("properties");
					InformalProperties.Builder properties = InformalProperties.Builder.ofJSON(jsonProperties);				
					informal.properties = properties;
					pointcloud.setInformal(informal.build());
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
		} finally {
			if(updateCatalog) {
				broker.catalog.update(pointcloud, updateCatalogPoints);
			}
		}
	}
}
