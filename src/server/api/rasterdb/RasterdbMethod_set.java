package server.api.rasterdb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.Informal.Builder;
import broker.InformalProperties;
import broker.TimeSlice;
import broker.TimeSlice.TimeSliceBuilder;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import rasterdb.Band;
import rasterdb.CustomWCS;
import rasterdb.CustomWMS;
import rasterdb.RasterDB;
import remotetask.rasterdb.ImportSpec;
import util.JsonUtil;
import util.Web;

public class RasterdbMethod_set extends RasterdbMethod {


	public RasterdbMethod_set(Broker broker) {
		super(broker, "set");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info(request);
		request.setHandled(true);
		boolean updateCatalog = false;
		boolean updateCatalogPoints = false;
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONObject meta = json.getJSONObject("meta");
			Iterator<String> it = meta.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "title": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String title = meta.getString("title").trim();
					Logger.info("set title " + title);
					Builder informal = rasterdb.informal().toBuilder();
					informal.title = title;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "description": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description").trim();
					Logger.info("set description " + description);
					Builder informal = rasterdb.informal().toBuilder();
					informal.description = description;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact").trim();
					Logger.info("set corresponding_contact " + corresponding_contact);
					Builder informal = rasterdb.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date").trim();
					Logger.info("set acquisition_date " + acquisition_date);
					Builder informal = rasterdb.informal().toBuilder();
					informal.acquisition_date = acquisition_date;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "proj4": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					updateCatalogPoints = true;
					String proj4 = meta.getString("proj4").trim();
					Logger.info("set proj4 "+proj4);
					rasterdb.setProj4(proj4);
					break;
				}
				case "code": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String code = meta.getString("code").trim();
					Logger.info("set code "+code);
					rasterdb.setCode(code);
					break;
				}
				case "acl": {
					ACL acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl"));
					if(!rasterdb.getACL().equals(acl)) {
						rasterdb.checkOwner(userIdentity, "set rasterdb acl");	
						updateCatalog = true;					
						rasterdb.setACL(acl);
					}
					break;
				}
				case "acl_mod": {
					ACL acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					if(!rasterdb.getACL_mod().equals(acl_mod)) {
						rasterdb.checkOwner(userIdentity, "set rasterdb acl_mod");
						updateCatalog = true;
						rasterdb.setACL_mod(acl_mod);
					}
					break;
				}
				case "acl_owner": {
					ACL acl_owner = ACL.ofRoles(JsonUtil.optStringTrimmedList(meta, "acl_owner"));
					if(!rasterdb.getACL_owner().equals(acl_owner)) {
						AclUtil.check(userIdentity, "set rasterdb acl_owner");
						updateCatalog = true;
						rasterdb.setACL_owner(acl_owner);
					}
					break;
				}
				case "tags": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					Builder informal = rasterdb.informal().toBuilder();
					informal.setTags(JsonUtil.optStringTrimmedArray(meta, "tags"));
					rasterdb.setInformal(informal.build());					
					break;
				}
				case "bands": {
					rasterdb.checkMod(userIdentity);
					JSONArray jsonBands = meta.getJSONArray("bands");
					int bandLen = jsonBands.length();
					for (int i = 0; i < bandLen; i++) {
						JSONObject jsonBand = jsonBands.getJSONObject(i);
						int index = jsonBand.getInt("index");
						Band band = rasterdb.getBandByNumber(index);
						if(band == null) {
							//throw new RuntimeException("band does not exist " + index);
							int type = 1;
							if(jsonBand.has("datatype")) {
								type = ImportSpec.parseBandDataType(JsonUtil.getString(jsonBand, "datatype"));
							}
							if(type < 1) {
								type = 1;
							}
							band = Band.of(type, index, "", null);
						}
						rasterdb.Band.Builder builder = new Band.Builder(band);
						Iterator<String> bandIt = jsonBand.keys();
						while(bandIt.hasNext()) {
							String bandKey = bandIt.next();
							switch(bandKey) {
							case "index": {
								if(builder.index != jsonBand.getInt("index")) {
									throw new RuntimeException("internal error");
								}
								break;
							}
							case "title": {
								builder.title  = jsonBand.getString("title").trim();
								break;
							}
							case "visualisation": {
								builder.visualisation  = jsonBand.getString("visualisation").trim();
								break;
							}
							case "vis_min": {
								builder.vis_min = JsonUtil.getDouble(jsonBand, "vis_min");
								break;
							}
							case "vis_max": {
								builder.vis_max = JsonUtil.getDouble(jsonBand, "vis_max");
								break;
							}
							case "wavelength": {
								builder.wavelength = JsonUtil.getDouble(jsonBand, "wavelength");
								break;
							}
							case "fwhm": {
								builder.fwhm = JsonUtil.getDouble(jsonBand, "fwhm");
								break;
							}
							case "datatype": {
								int bandDataType = ImportSpec.parseBandDataType(JsonUtil.getString(jsonBand, "datatype"));
								if(bandDataType > 0 && bandDataType != builder.type) {
									Logger.warn("band data type can be set at initial band creation only");
								}
								break;
							}
							default: {					
								Logger.warn("unknown band key: "+bandKey);
								//throw new RuntimeException("unknown key: "+key);
							}
							}
						}
						rasterdb.setBand(builder.build(), true);
					}
					Logger.info(jsonBands);
					break;
				}
				case "time_slices": {
					rasterdb.checkMod(userIdentity);
					JSONArray jsonTimeSlices = meta.getJSONArray("time_slices");
					int timeSlicesLen = jsonTimeSlices.length();

					Map<Integer, TimeSliceBuilder> changeMap = new LinkedHashMap<Integer, TimeSliceBuilder>();

					for (int i = 0; i < timeSlicesLen; i++) {
						JSONObject jsonTimeSlice = jsonTimeSlices.getJSONObject(i);
						int id = jsonTimeSlice.getInt("id");
						String name = jsonTimeSlice.getString("name").trim();

						TimeSliceBuilder timeSliceBuilder = changeMap.get(id);
						if(timeSliceBuilder == null) {
							TimeSlice timeSlice = rasterdb.timeMapReadonly.get(id);
							if(timeSlice == null) {
								timeSliceBuilder = new TimeSliceBuilder(name);
							} else {
								timeSliceBuilder = TimeSliceBuilder.of(timeSlice);
							}
							changeMap.put(id, timeSliceBuilder);
						}

						timeSliceBuilder.name = name;
					}
					List<TimeSlice> timeSlices = changeMap.entrySet().stream().map(e -> {
						int id = e.getKey();
						TimeSliceBuilder timeSliceBuilder = e.getValue();
						return new TimeSlice(id, timeSliceBuilder);
					}).collect(Collectors.toList());
					rasterdb.setTimeSlices(timeSlices);
					Logger.info(jsonTimeSlices);
					break;
				}				
				case "associated": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					JSONObject jsonAssociated = meta.getJSONObject("associated");
					rasterdb.setAssociatedPointDB(jsonAssociated.optString("PointDB", ""));
					rasterdb.setAssociatedPointCloud(jsonAssociated.optString("pointcloud", ""));
					rasterdb.setAssociatedVoxelDB(jsonAssociated.optString("voxeldb", ""));
					rasterdb.setAssociatedPoiGroups(JsonUtil.optStringTrimmedList(jsonAssociated, "poi_groups"));
					rasterdb.setAssociatedRoiGroups(JsonUtil.optStringTrimmedList(jsonAssociated, "roi_groups"));					
					break;
				}
				case "properties": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;	
					Builder informal = rasterdb.informal().toBuilder();
					JSONObject jsonProperties = meta.getJSONObject("properties");
					InformalProperties.Builder properties = InformalProperties.Builder.ofJSON(jsonProperties);				
					informal.properties = properties;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "custom_wms": {
					rasterdb.checkMod(userIdentity);
					HashMap<String, CustomWMS> map = JsonUtil.parsePropertyHashMap(meta.getJSONObject("custom_wms"), CustomWMS::ofJSON);
					rasterdb.setCustomWMS(map);
					break;
				}
				case "custom_wcs": {
					rasterdb.checkMod(userIdentity);
					HashMap<String, CustomWCS> map = JsonUtil.parsePropertyHashMap(meta.getJSONObject("custom_wcs"), CustomWCS::ofJSON);
					rasterdb.setCustomWCS(map);
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

			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("response");
			json.object();
			json.endObject();
			json.endObject();
		} finally {
			if(updateCatalog) {
				broker.catalog.update(rasterdb, updateCatalogPoints);
			}
		}		
	}
}