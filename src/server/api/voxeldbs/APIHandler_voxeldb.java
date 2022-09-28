package server.api.voxeldbs;

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
import broker.Informal.Builder;
import broker.TimeSlice;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import util.Extent3d;
import util.JsonUtil;
import util.Range3d;
import util.Web;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class APIHandler_voxeldb {


	protected static final String MIME_JSON = "application/json";

	private final Broker broker;

	private final Handler_voxels handler_voxels = new Handler_voxels();
	private final Handler_aggregated_voxels handler_aggregated_voxels = new Handler_aggregated_voxels();
	private final Handler_raster handler_raster = new Handler_raster();

	public APIHandler_voxeldb(Broker broker) {
		this.broker = broker;
	}

	public void handle(String name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		//Logger.info("get: " + name);
		VoxelDB voxeldb = broker.getVoxeldb(name);
		if(voxeldb == null) {
			throw new RuntimeException("VoxelDB not found: " + name);
		} else if(target.equals("/")) {
			switch(request.getMethod()) {
			case "GET":
				voxeldb.check(userIdentity);
				handleGET(voxeldb, request, response, userIdentity);
				break;
			case "POST":
				voxeldb.checkMod(userIdentity);
				handlePOST(voxeldb, request, response, userIdentity);
				break;
			default:
				throw new RuntimeException("invalid HTTP method: " + request.getMethod());
			}
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name in voxeldb: " + target);
			}			
			String resource = i < 0 ? target.substring(1) : target.substring(1, i);
			int formatIndex = resource.lastIndexOf('.');
			String resourceName = formatIndex < 0 ? resource : resource.substring(0, formatIndex);
			/*String resourceFormat = formatIndex < 0 ? "" : resource.substring(formatIndex + 1);
			Logger.info("resourceName: "+resourceName);
			Logger.info("resourceFormat: "+resourceFormat);*/
			String next = i < 0 ? "/" : target.substring(i);
			if(next.equals("/")) {
				switch(resourceName) {
				case "voxels":
					handler_voxels.handle(voxeldb, request, response, userIdentity);
					break;
				case "aggregated_voxels":
					handler_aggregated_voxels.handle(voxeldb, request, response, userIdentity);
					break;	
				case "raster":
					handler_raster.handle(voxeldb, request, response, userIdentity);
					break;						
				default:
					throw new RuntimeException("unknown resource: " + resource);
				}
			} else {
				throw new RuntimeException("error in subpath: " + target);
			}
		}		
	}

	private void handleGET(VoxelDB voxeldb, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		
		boolean storage_measures = request.getParameter("storage_measures") != null;
		boolean extent = request.getParameter("extent") != null;

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("voxeldb");
		json.object();
		json.key("name");
		json.value(voxeldb.config.name);
		voxeldb.informal().writeJson(json);

		json.key("cell_size");
		json.object();
		json.key("x");
		json.value(voxeldb.getCellsize());
		json.key("y");
		json.value(voxeldb.getCellsize());
		json.key("z");
		json.value(voxeldb.getCellsize());
		json.endObject();

		VoxelGeoRef ref = voxeldb.geoRef();
		json.key("ref");
		json.object();

		json.key("epsg");
		json.value(ref.epsg);
		json.key("proj4");
		json.value(ref.proj4);



		Unit unit = null;
		if(ref.hasProj4()) {
			try {
				CoordinateReferenceSystem crs = new CRSFactory().createFromParameters(null, ref.proj4);
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
		if(unit == null && voxeldb.geoRef().hasEpsg()) {
			try {
				CoordinateReferenceSystem crs = new CRSFactory().createFromName("EPSG:" + ref.epsg);
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

		json.key("voxel_size");
		json.object();
		json.key("x");
		json.value(ref.voxelSizeX);
		json.key("y");
		json.value(ref.voxelSizeY);
		json.key("z");
		json.value(ref.voxelSizeZ);
		json.endObject();

		json.key("origin");
		json.object();
		json.key("x");
		json.value(ref.originX);
		json.key("y");
		json.value(ref.originY);
		json.key("z");
		json.value(ref.originZ);
		json.endObject();

		json.endObject();

		if(extent) {
			Range3d local_range = voxeldb.getLocalRange(false);
			if(local_range != null) {
				json.key("local_range");
				local_range.toJSON(json);
				Extent3d extent3d = ref.toGeoExtent(local_range);
				json.key("extent");
				extent3d.toJSON(json);
			}
		}

		if(storage_measures) {
			json.key("storage_measures");
			json.object();
			json.key("cell_count");
			int cell_count = voxeldb.getGriddb().storage().calculateTileCount();
			json.value(cell_count);
			try {
				long storage_size = voxeldb.getGriddb().storage().calculateStorageSize();
				json.key("storage_size");
				json.value(storage_size);
			} catch(Exception e) {
				Logger.warn(e);
			}
			try {
				long internal_free_size = voxeldb.getGriddb().storage().calculateInternalFreeSize();
				json.key("storage_internal_free_size");
				json.value(internal_free_size);
			} catch(Exception e) {
				Logger.warn(e);
			}
			long[] stats = voxeldb.getGriddb().storage().calculateTileSizeStats();
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


			Range3d cell_range = new CellFactory(voxeldb).getCellRange();
			if(cell_range != null) {
				json.key("cell_range");
				cell_range.toJSON(json);
			}

			Range3d local_range = voxeldb.getLocalRange(false);
			if(local_range != null) {
				json.key("local_range");
				local_range.toJSON(json);
				Extent3d extent3d = ref.toGeoExtent(local_range);
				json.key("extent");
				extent3d.toJSON(json);
			}

			json.endObject();
		}

		json.key("modify");
		json.value(voxeldb.isAllowedMod(userIdentity));
		json.key("owner");
		json.value(voxeldb.isAllowedOwner(userIdentity));
		json.key("acl");
		voxeldb.getACL().writeJSON(json);
		json.key("acl_mod");
		voxeldb.getACL_mod().writeJSON(json);
		json.key("acl_owner");
		voxeldb.getACL_owner().writeJSON(json);
		json.key("attributes");
		json.array();
		voxeldb.getGriddb().getAttributes().forEach(attribute -> json.value(attribute.name));
		json.endArray();
		json.key("associated");
		voxeldb.getAssociated().writeJSON(json);

		json.key("time_slices");
		TimeSlice.timeSlicesToJSON(voxeldb.timeMapReadonly.values(), json);		

		json.endObject(); // voxeldb
		json.endObject(); // JSON
	}

	private void handlePOST(VoxelDB voxeldb, Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		
		boolean updateCatalog = false;
		boolean updateCatalogPoints = false;
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONObject meta = json.getJSONObject("voxeldb");
			Iterator<String> it = meta.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "title": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String title = meta.getString("title");
					Logger.info("set title " + title);
					Builder informal = voxeldb.informal().toBuilder();
					informal.title = title.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "description": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description");
					Logger.info("set description " + description);
					Builder informal = voxeldb.informal().toBuilder();
					informal.description = description.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact");
					Logger.info("set corresponding_contact " + corresponding_contact);
					Builder informal = voxeldb.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date");
					Logger.info("set acquisition_date " + acquisition_date);
					Builder informal = voxeldb.informal().toBuilder();
					informal.acquisition_date = acquisition_date.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "epsg": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					int epsg = Integer.parseInt(meta.get("epsg").toString().trim());
					Logger.info("set epsg " + epsg);
					voxeldb.setEpsg(epsg);
					break;
				}
				case "proj4": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					updateCatalogPoints = true;
					String proj4 = meta.getString("proj4").trim();
					Logger.info("set proj4 "+proj4);
					voxeldb.setProj4(proj4);
					break;
				}
				case "acl": {
					ACL acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl"));
					if(!voxeldb.getACL().equals(acl)) {
						voxeldb.checkOwner(userIdentity, "set voxeldb acl");
						updateCatalog = true;
						voxeldb.setACL(acl);
					}
					break;
				}
				case "acl_mod": {
					ACL acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					if(!voxeldb.getACL_mod().equals(acl_mod)) {
						voxeldb.checkOwner(userIdentity, "set voxeldb acl_mod");	
						updateCatalog = true;
						voxeldb.setACL_mod(acl_mod);
					}
					break;
				}
				case "acl_owner": {
					ACL acl_owner = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_owner"));
					if(!voxeldb.getACL_owner().equals(acl_owner)) {
						AclUtil.check(userIdentity, "set voxeldb acl_owner");	
						updateCatalog = true;
						voxeldb.setACL_owner(acl_owner);
					}
					break;
				}				
				case "tags": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					Builder informal = voxeldb.informal().toBuilder();
					informal.setTags(JsonUtil.optStringTrimmedArray(meta, "tags"));
					voxeldb.setInformal(informal.build());
					break;
				}
				case "associated": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					JSONObject jsonAssociated = meta.getJSONObject("associated");
					voxeldb.setAssociatedRasterDB(jsonAssociated.optString("rasterdb", ""));
					voxeldb.setAssociatedPoiGroups(JsonUtil.optStringTrimmedList(jsonAssociated, "poi_groups"));
					voxeldb.setAssociatedRoiGroups(JsonUtil.optStringTrimmedList(jsonAssociated, "roi_groups"));
					break;
				}
				case "delete_voxeldb": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = false; // will be updated by delete voxeldb
					String voxeldb_name = meta.getString("delete_voxeldb");
					if(!voxeldb_name.equals(voxeldb.getName())) {
						throw new RuntimeException("wrong parameters for delete voxeldb: " + voxeldb_name);
					}
					Logger.info("delete voxeldb " + voxeldb_name);
					broker.deleteVoxeldb(voxeldb.getName());
					voxeldb = null;
					break;
				}
				case "properties": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;	
					Builder informal = voxeldb.informal().toBuilder();
					JSONObject jsonProperties = meta.getJSONObject("properties");
					InformalProperties.Builder properties = InformalProperties.Builder.ofJSON(jsonProperties);				
					informal.properties = properties;
					voxeldb.setInformal(informal.build());
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
				broker.catalog.update(voxeldb, updateCatalogPoints);
			}
		}
	}
}
