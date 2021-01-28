package server.api.rasterdb;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import rasterdb.Band;
import rasterdb.RasterDB;
import remotetask.rasterdb.ImportSpec;
import util.JsonUtil;
import util.Web;

public class RasterdbMethod_set extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_set(Broker broker) {
		super(broker, "set");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.info(request);
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
					log.info("set title " + title);
					Builder informal = rasterdb.informal().toBuilder();
					informal.title = title;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "description": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String description = meta.getString("description").trim();
					log.info("set description " + description);
					Builder informal = rasterdb.informal().toBuilder();
					informal.description = description;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "corresponding_contact": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String corresponding_contact = meta.getString("corresponding_contact").trim();
					log.info("set corresponding_contact " + corresponding_contact);
					Builder informal = rasterdb.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "acquisition_date": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String acquisition_date = meta.getString("acquisition_date").trim();
					log.info("set acquisition_date " + acquisition_date);
					Builder informal = rasterdb.informal().toBuilder();
					informal.acquisition_date = acquisition_date;
					rasterdb.setInformal(informal.build());
					break;
				}
				
				
				case "Source": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Source = meta.getString("Source").trim();
					informal.dc_Source = dc_Source;
					rasterdb.setInformal(informal.build());
					break;
				}				
				case "Relation": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Relation = meta.getString("Relation").trim();
					informal.dc_Relation = dc_Relation;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "Coverage": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Coverage = meta.getString("Coverage").trim();
					informal.dc_Coverage = dc_Coverage;
					rasterdb.setInformal(informal.build());
					break;
				}				
				case "Creator": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Creator = meta.getString("Creator").trim();
					informal.dc_Creator = dc_Creator;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "Contributor": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Contributor = meta.getString("Contributor").trim();
					informal.dc_Contributor = dc_Contributor;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "Rights": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Rights = meta.getString("Rights").trim();
					informal.dc_Rights = dc_Rights;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "Audience": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Audience = meta.getString("Audience").trim();
					informal.dc_Audience = dc_Audience;
					rasterdb.setInformal(informal.build());
					break;
				}
				case "Provenance": {
					rasterdb.checkMod(userIdentity);
					Builder informal = rasterdb.informal().toBuilder();
					String dc_Provenance = meta.getString("Provenance").trim();
					informal.dc_Provenance = dc_Provenance;
					rasterdb.setInformal(informal.build());
					break;
				}				
				
				
				case "proj4": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					updateCatalogPoints = true;
					String proj4 = meta.getString("proj4").trim();
					log.info("set proj4 "+proj4);
					rasterdb.setProj4(proj4);
					break;
				}
				case "code": {
					rasterdb.checkMod(userIdentity);
					updateCatalog = true;
					String code = meta.getString("code").trim();
					log.info("set code "+code);
					rasterdb.setCode(code);
					break;
				}
				case "acl": {
					EmptyACL.ADMIN.check(userIdentity);	
					updateCatalog = true;
					ACL acl = ACL.of(JsonUtil.optStringTrimmedList(meta, "acl"));
					rasterdb.setACL(acl);
					break;
				}
				case "acl_mod": {
					EmptyACL.ADMIN.check(userIdentity);	
					updateCatalog = true;
					ACL acl_mod = ACL.of(JsonUtil.optStringTrimmedList(meta, "acl_mod"));
					rasterdb.setACL_mod(acl_mod);
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
									log.warn("band data type can be set at initial band creation only");
								}
								break;
							}
							default: {					
								log.warn("unknown band key: "+bandKey);
								//throw new RuntimeException("unknown key: "+key);
							}
							}
						}
						rasterdb.setBand(builder.build());
					}
					log.info(jsonBands);
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

			response.setContentType(MIME_JSON);
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