package server.api.pointclouds;

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

import broker.Broker;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import broker.group.PoiGroup;
import broker.group.RoiGroup;
import pointcloud.DoubleRect;
import pointcloud.PointCloud;
import util.JsonUtil;
import util.Range2d;
import util.Web;

public class APIHandler_pointcloud {
	private static final Logger log = LogManager.getLogger();

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
		log.info("get: " + name);
		PointCloud pointcloud = broker.getPointCloud(name);
		log.info("get: " + pointcloud);
		if(pointcloud == null) {
			throw new RuntimeException("PointCloud not found: " + name);
			/*log.error("PointCloud not found: " + name);
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
			log.info("resourceName: "+resourceName);
			log.info("resourceFormat: "+resourceFormat);
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
		json.key("cell_size");
		json.value(pointcloud.getCellsize());
		json.key("cell_scale");
		json.value(pointcloud.getCellscale());
		if(requestExtent) {
			json.key("extent");
			json.array();
			json.value(range.xmin);
			json.value(range.ymin);
			json.value(range.xmax);
			json.value(range.ymax);
			json.endArray();
			json.key("cell_extent");
			json.array();
			json.value(cell_range.xmin);
			json.value(cell_range.ymin);
			json.value(cell_range.xmax);
			json.value(cell_range.ymax);
			json.endArray();
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
		if(EmptyACL.ADMIN.isAllowed(userIdentity)) {
			json.key("acl");
			pointcloud.getACL().writeJSON(json);
			json.key("acl_mod");
			pointcloud.getACL_mod().writeJSON(json);
		}
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
					log.warn(e);
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
					log.warn(e);
				}
			}
			json.endArray();
		}
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
					log.info("set title " + title);
					Builder informal = pointcloud.informal().toBuilder();
					informal.title = title.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "description": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description");
					log.info("set description " + description);
					Builder informal = pointcloud.informal().toBuilder();
					informal.description = description.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact");
					log.info("set corresponding_contact " + corresponding_contact);
					Builder informal = pointcloud.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date");
					log.info("set acquisition_date " + acquisition_date);
					Builder informal = pointcloud.informal().toBuilder();
					informal.acquisition_date = acquisition_date.trim();
					pointcloud.setInformal(informal.build());
					break;
				}
				case "code": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					String code = meta.getString("code").trim();
					log.info("set code "+code);
					pointcloud.setCode(code);
					break;
				}
				case "proj4": {
					pointcloud.checkMod(userIdentity);
					updateCatalog = true;
					updateCatalogPoints = true;
					String proj4 = meta.getString("proj4").trim();
					log.info("set proj4 "+proj4);
					pointcloud.setProj4(proj4);
					break;
				}
				case "acl": {
					EmptyACL.ADMIN.check(userIdentity);	
					updateCatalog = true;									
					ACL acl = ACL.of(JsonUtil.optStringTrimmedList(meta, "acl"));
					pointcloud.setACL(acl);
					break;
				}
				case "acl_mod": {
					EmptyACL.ADMIN.check(userIdentity);	
					updateCatalog = true;		
					ACL acl_mod = ACL.of(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					pointcloud.setACL_mod(acl_mod);
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
				broker.catalog.update(pointcloud, updateCatalogPoints);
			}
		}
	}
}
