package server.api.vectordbs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.Informal.Builder;
import broker.InformalProperties;
import broker.acl.ACL;
import broker.acl.AclUtil;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import util.JsonUtil;
import util.Util;
import util.Web;
import vectordb.VectorDB;
import vectordb.style.Style;

public class VectordbHandler_root extends VectordbHandler {


	public VectordbHandler_root(Broker broker) {
		super(broker, "");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			boolean data_filenames = true;
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("vectordb");
			json.object();
			JsonUtil.put(json, "name", vectordb.getName());

			json.key("modify");
			json.value(vectordb.isAllowedMod(userIdentity));
			json.key("owner");
			json.value(vectordb.isAllowedOwner(userIdentity));
			json.key("acl");
			vectordb.getACL().writeJSON(json);
			json.key("acl_mod");
			vectordb.getACL_mod().writeJSON(json);
			json.key("acl_owner");
			vectordb.getACL_owner().writeJSON(json);

			vectordb.informal().writeJson(json);

			json.key("data_filename");
			json.value(vectordb.getDataFilename());

			json.key("data_filenames");
			json.array();
			if(data_filenames) {
				List<Path> files = vectordb.getDataFilenames();				
				files.stream().map(Path::toString).sorted(String.CASE_INSENSITIVE_ORDER).forEach(json::value);
			}
			json.endArray();

			json.key("datatag");
			json.value(vectordb.getDatatag());

			VectordbDetails details = vectordb.getDetails();
			json.key("details");
			json.object();
			json.key("epsg");
			json.value(details.epsg);
			json.key("proj4");
			json.value(details.proj4);
			json.key("attributes");
			json.value(details.attributes.stream().sorted(String.CASE_INSENSITIVE_ORDER).toArray());
			json.endObject();

			if(Web.getFlagBoolean(request, "extent")) {
				Rect2d extent = vectordb.getExtent();
				if(extent != null) {
					json.key("extent");
					extent.toJSON(json);
				}
			}

			json.key("name_attribute");
			json.value(vectordb.getNameAttribute());

			json.key("structured_access");
			json.object();
			json.key("poi");
			json.value(vectordb.getStructuredAccessPOI());
			json.key("roi");
			json.value(vectordb.getStructuredAccessROI());
			json.endObject();

			if(vectordb.getStyle() != null) {
				json.key("style");
				vectordb.getStyle().writeJson(json);
			}

			json.endObject(); // vectordb object	
			json.endObject(); // JSON object

		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}	
	}

	@Override
	public void handleDELETE(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {	
		Logger.info("delete");
		vectordb.checkMod(userIdentity);
		if(broker.deleteVectordb(vectordb.getName())) {
			response.setContentType(Web.MIME_TEXT);
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setContentType(Web.MIME_TEXT);
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			response.getWriter().print("could not delete");
		}
	}

	private final static Set<String> POST_PROPS_MANDATORY = Util.of();
	private final static Set<String> POST_PROPS = Util.of("data_filename", "title", "description", "tags", "acl", "acl_mod", "acl_owner", "corresponding_contact", "acquisition_date", "name_attribute", "structured_access", "name", "properties", "style");

	private final static Set<String> STRUCTURED_ACCESS_PROPS_MANDATORY = Util.of();
	private final static Set<String> STRUCTURED_ACCESS_PROPS = Util.of("poi", "roi");

	@Override
	public void handlePOST(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			response.setContentType(Web.MIME_TEXT);
			response.setStatus(HttpServletResponse.SC_OK);
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			Util.checkProps(POST_PROPS_MANDATORY, POST_PROPS, json);
			updateVectordbProps(vectordb, json, userIdentity, false);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}
	}

	public void updateVectordbProps(VectorDB vectordb, JSONObject json,  UserIdentity userIdentity, boolean isNewCreated) {
		boolean writeMeta = false;
		boolean updateCatalog = false;
		boolean refreshDatatag = false;
		boolean refreshPoiGroups = false;
		boolean refreshRoiGroups = false;
		try {
			Iterator<String> it = json.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "name": {
					String name = JsonUtil.getString(json, "name");
					if(!vectordb.getName().equals(name)) {
						throw new RuntimeException("vectordb name can not change");
					}
					break;
				}
				case "data_filename": {
					vectordb.checkMod(userIdentity);
					String name = JsonUtil.getString(json, "data_filename");
					vectordb.setDataFilename(name);
					writeMeta = true;
					refreshDatatag = true;
					break;
				}
				case "name_attribute": {
					vectordb.checkMod(userIdentity);
					String name = JsonUtil.getString(json, "name_attribute");
					vectordb.setNameAttribute(name);
					writeMeta = true;
					break;
				}
				case "title": {
					vectordb.checkMod(userIdentity);
					String title = json.getString("title");
					Builder informal = vectordb.informal().toBuilder();
					informal.title = title.trim();
					vectordb.setInformal(informal.build());
					updateCatalog = true;
					writeMeta = true;
					break;
				}
				case "description": {
					vectordb.checkMod(userIdentity);
					String description = json.getString("description");
					Builder informal = vectordb.informal().toBuilder();
					informal.description = description.trim();
					vectordb.setInformal(informal.build());
					updateCatalog = true;
					writeMeta = true;
					break;
				}
				case "corresponding_contact": {
					vectordb.checkMod(userIdentity);
					String corresponding_contact = json.getString("corresponding_contact");
					Builder informal = vectordb.informal().toBuilder();
					informal.corresponding_contact = corresponding_contact.trim();
					vectordb.setInformal(informal.build());
					updateCatalog = true;
					writeMeta = true;
					break;
				}
				case "acquisition_date": {
					vectordb.checkMod(userIdentity);
					String acquisition_date = json.getString("acquisition_date");
					Builder informal = vectordb.informal().toBuilder();
					informal.acquisition_date = acquisition_date.trim();
					vectordb.setInformal(informal.build());
					updateCatalog = true;
					writeMeta = true;
					break;
				}
				case "tags": {
					vectordb.checkMod(userIdentity);
					Builder informal = vectordb.informal().toBuilder();
					informal.setTags(JsonUtil.optStringTrimmedArray(json, "tags"));
					vectordb.setInformal(informal.build());
					updateCatalog = true;
					writeMeta = true;
					break;
				}
				case "acl": {
					ACL acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(json, "acl"));
					if(!vectordb.getACL().equals(acl)) {
						if(!isNewCreated) {
							vectordb.checkOwner(userIdentity, "set vectordb acl_mod");
						}
						vectordb.setACL(acl);
						updateCatalog = true;
						writeMeta = true;
					}
					break;
				}
				case "acl_mod": {
					ACL acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(json, "acl_mod"));
					if(!vectordb.getACL_mod().equals(acl_mod)) {
						if(!isNewCreated) {
							vectordb.checkOwner(userIdentity, "set vectordb acl_mod");
						}
						vectordb.setACL_mod(acl_mod);
						updateCatalog = true;
						writeMeta = true;
					}
					break;
				}
				case "acl_owner": {
					ACL acl_owner = ACL.ofRoles(JsonUtil.optStringTrimmedList(json, "acl_owner"));
					if(!vectordb.getACL_owner().equals(acl_owner)) {
						if(!isNewCreated) {
							AclUtil.check(userIdentity, "set vectordb acl_owner");
						}
						vectordb.setACL_owner(acl_owner);
						updateCatalog = true;
						writeMeta = true;
					}
					break;
				}
				case "structured_access": {
					JSONObject st = json.getJSONObject("structured_access");
					Util.checkProps(STRUCTURED_ACCESS_PROPS_MANDATORY, STRUCTURED_ACCESS_PROPS, st);
					if(st.has("poi")) {
						vectordb.setStructuredAccessPOI(st.getBoolean("poi"));
						writeMeta = true;
						updateCatalog = true;
						refreshPoiGroups = true;
					}
					if(st.has("roi")) {
						vectordb.setStructuredAccessROI(st.getBoolean("roi"));
						writeMeta = true;
						updateCatalog = true;
						refreshRoiGroups = true;
					}
					break;
				}
				case "properties": {
					vectordb.checkMod(userIdentity);
					Builder informal = vectordb.informal().toBuilder();
					JSONObject jsonProperties = json.getJSONObject("properties");
					InformalProperties.Builder properties = InformalProperties.Builder.ofJSON(jsonProperties);				
					informal.properties = properties;
					vectordb.setInformal(informal.build());
					updateCatalog = true;
					writeMeta = true;
					break;
				}
				case "style": {
					vectordb.checkMod(userIdentity);
					Style style = Style.ofJSON(json.getJSONObject("style"));
					vectordb.setStyle(style);				
					writeMeta = true;
					break;
				}				
				default: 
					throw new RuntimeException("unknown key: "+key);
				}
			}
		} finally {
			if(writeMeta) {
				vectordb.writeMeta();
			}
			if(refreshDatatag) {
				vectordb.refreshDatatag();
			}
			if(updateCatalog) {
				broker.catalog.update(vectordb, false);
			}
			if(refreshPoiGroups) {
				broker.refreshPoiGroupMap();
			}
			if(refreshRoiGroups) {
				broker.refreshRoiGroupMap();
			}
		}
	}
}
