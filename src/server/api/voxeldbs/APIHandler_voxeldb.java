package server.api.voxeldbs;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import broker.TimeSlice;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import rasterunit.KeyRange;
import util.JsonUtil;
import util.Web;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class APIHandler_voxeldb {
	private static final Logger log = LogManager.getLogger();

	protected static final String MIME_JSON = "application/json";

	private final Broker broker;

	private final Handler_voxels handler_voxels = new Handler_voxels();
	private final Handler_aggregated_voxels handler_aggregated_voxels = new Handler_aggregated_voxels();

	public APIHandler_voxeldb(Broker broker) {
		this.broker = broker;
	}

	public void handle(String name, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		//log.info("get: " + name);
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
			log.info("resourceName: "+resourceName);
			log.info("resourceFormat: "+resourceFormat);*/
			String next = i < 0 ? "/" : target.substring(i);
			if(next.equals("/")) {
				switch(resourceName) {
				case "voxels":
					handler_voxels.handle(voxeldb, request, response, userIdentity);
					break;
				case "aggregated_voxels":
					handler_aggregated_voxels.handle(voxeldb, request, response, userIdentity);
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
				log.warn(e);
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
				log.warn(e);
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
				log.warn(e);
			}
			try {
				long internal_free_size = voxeldb.getGriddb().storage().calculateInternalFreeSize();
				json.key("storage_internal_free_size");
				json.value(internal_free_size);
			} catch(Exception e) {
				log.warn(e);
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

			KeyRange keyRange = voxeldb.getGriddb().storage().getKeyRange();
			if(keyRange != null) {
				json.key("cell_range");
				json.object();
				json.key("xmin");
				json.value(keyRange.xmin);	
				json.key("xmax");
				json.value(keyRange.xmax);	
				json.key("ymin");
				json.value(keyRange.ymin);	
				json.key("ymax");
				json.value(keyRange.ymax);
				json.key("zmin");
				json.value(keyRange.bmin);	
				json.key("zmax");
				json.value(keyRange.bmax);
				json.endObject();	
			}

			json.endObject();
		}

		json.key("modify");
		json.value(voxeldb.isAllowedMod(userIdentity));
		//if(EmptyACL.ADMIN.isAllowed(userIdentity)) {
		json.key("acl");
		voxeldb.getACL().writeJSON(json);
		json.key("acl_mod");
		voxeldb.getACL_mod().writeJSON(json);
		//}
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
					log.info("set title " + title);
					Builder informal = voxeldb.informal().toBuilder();
					informal.title = title.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "description": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description");
					log.info("set description " + description);
					Builder informal = voxeldb.informal().toBuilder();
					informal.description = description.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact");
					log.info("set corresponding_contact " + corresponding_contact);
					Builder informal = voxeldb.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date");
					log.info("set acquisition_date " + acquisition_date);
					Builder informal = voxeldb.informal().toBuilder();
					informal.acquisition_date = acquisition_date.trim();
					voxeldb.setInformal(informal.build());
					break;
				}
				case "epsg": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					int epsg = Integer.parseInt(meta.getString("epsg").trim());
					log.info("set epsg " + epsg);
					voxeldb.setEpsg(epsg);
					break;
				}
				case "proj4": {
					voxeldb.checkMod(userIdentity);
					updateCatalog = true;
					updateCatalogPoints = true;
					String proj4 = meta.getString("proj4").trim();
					log.info("set proj4 "+proj4);
					voxeldb.setProj4(proj4);
					break;
				}
				case "acl": {
					EmptyACL.ADMIN.check(userIdentity);	
					updateCatalog = true;									
					ACL acl = ACL.of(JsonUtil.optStringTrimmedList(meta, "acl"));
					voxeldb.setACL(acl);
					break;
				}
				case "acl_mod": {
					EmptyACL.ADMIN.check(userIdentity);	
					updateCatalog = true;		
					ACL acl_mod = ACL.of(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					voxeldb.setACL_mod(acl_mod);
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
					log.info("delete voxeldb " + voxeldb_name);
					broker.deleteVoxeldb(voxeldb.getName());
					voxeldb = null;
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
			log.error(e);			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().append(e.toString());
		} finally {
			if(updateCatalog) {
				broker.catalog.update(voxeldb, updateCatalogPoints);
			}
		}
	}
}
