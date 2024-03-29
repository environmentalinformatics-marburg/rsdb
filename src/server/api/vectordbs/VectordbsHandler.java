package server.api.vectordbs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.ACL;
import broker.catalog.CatalogKey;
import util.JsonUtil;
import util.Util;
import util.Web;
import vectordb.VectorDB;

public class VectordbsHandler extends AbstractHandler {
	
	HashMap<String, VectordbHandler> methodMap = new HashMap<String, VectordbHandler>();

	private final Broker broker;
	private final VectordbHandler_root vectordbHandler_root;

	public VectordbsHandler(Broker broker) {
		this.broker = broker;
		vectordbHandler_root = new VectordbHandler_root(broker);
		addMethod(vectordbHandler_root);
		addMethod(new VectordbHandler_geometry(broker));
		addMethod(new VectordbHandler_table(broker));
		addMethod(new VectordbHandler_files(broker));
		addMethod(new VectordbHandler_package_zip(broker));
		addMethod(new VectordbHandler_raster_png(broker));
		addMethod(new VectordbHandler_wfs(broker));
		addMethod(new VectordbHandler_wms(broker));
	}

	private void addMethod(VectordbHandler handler) {
		addMethod(handler, handler.getMethod());
	}

	private void addMethod(VectordbHandler handler, String apiMethod) {
		String name = apiMethod;
		if (methodMap.containsKey(name)) {
			Logger.warn("method name already exists overwrite '" + name + "'  " + methodMap.get(name) + "  " + handler);
		}
		methodMap.put(name, handler);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Response baseResponse = (Response) response;
		baseRequest.setHandled(true);
		//Logger.info("vectordb request " + target);

		String currTarget = target.charAt(0) == '/' ? target.substring(1) : target;
		//Logger.info(MARKER_API, Web.getRequestLogString("API", currTarget, baseRequest));
		//Logger.info("currTarget " + currTarget);
		int nameSepIndex = currTarget.indexOf('/');
		String name = nameSepIndex < 0 ? currTarget : currTarget.substring(0, nameSepIndex); 
		//Logger.info("name " + name);
		String subTarget = nameSepIndex < 0 ? "" : currTarget.substring(nameSepIndex + 1);
		//Logger.info("subTarget " + subTarget);

		int methodSepIndex = subTarget.indexOf('/');
		String method = methodSepIndex < 0 ? subTarget : subTarget.substring(0, methodSepIndex); 
		//Logger.info("method " + method);
		String subsubTarget = methodSepIndex < 0 ? "" : subTarget.substring(methodSepIndex + 1);
		//Logger.info("subsubTarget " + subsubTarget);

		UserIdentity userIdentity = Web.getUserIdentity(baseRequest);
		//UserIdentity userIdentity = null;

		if(name.isEmpty()) {
			handleRoot(baseRequest, baseResponse, userIdentity);
			return;
		}
		VectordbHandler handler = methodMap.get(method);
		if(handler == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("unknown method " + method);
			return;
		}
		VectorDB vectordb;
		try {
			vectordb = broker.getVectorDB(name);
		} catch(Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e.getMessage());
			return;
		}		
		if(vectordb.isAllowed(userIdentity)) {
			try {
				handler.handle(vectordb, subsubTarget, baseRequest, (Response) response, userIdentity);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.error(e);
				try {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.setContentType(Web.MIME_TEXT);
					response.getWriter().println(e.getMessage());
				} catch(Exception e1) {
					Logger.error(e1);
				}
			}
		} else {
			Logger.error("access not allowed for user");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("access not allowed for user");
		}

	}

	public void handleRoot(Request request, Response response, UserIdentity userIdentity) throws IOException {
		String method = request.getMethod();
		switch(method) {
		case "GET":
			handleRootGET(request, response, userIdentity);
			return;
		case "POST":
			handleRootPOST(request, response, userIdentity);
			return;
		default:
			Logger.error("HTTP method not allowed");
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("HTTP method not allowed");
			return;
		}	
	}

	public void handleRootGET(Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			boolean withDescription = request.getParameter("description") != null;
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("vectordbs");
			json.array();
			broker.catalog.getSorted(CatalogKey.TYPE_VECTORDB, userIdentity).forEach(entry -> {
				json.object();
				JsonUtil.put(json, "name", entry.name);
				if(entry.title != null && !entry.title.isEmpty()) {
					JsonUtil.put(json, "title", entry.title);
				}
				JsonUtil.writeOptArray(json, "tags", entry.tags);
				if(withDescription) {
					JsonUtil.optPut(json, "description", entry.description);	
				}
				json.endObject();
			});
			json.endArray();			
			json.endObject();

		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}	

	private final static Set<String> CREATE_VECTORDB_PROPS_MANDATORY = Util.of("name");
	private final static Set<String> CREATE_VECTORDB_PROPS = Util.of(CREATE_VECTORDB_PROPS_MANDATORY, "acl", "acl_mod");	

	public void handleRootPOST(Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {			
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			Iterator<String> it = json.keys();
			while(it.hasNext()) {
				String key = it.next();
				switch(key) {
				case "create_vectordb": {
					JSONObject props = json.getJSONObject(key);
					Util.checkProps(CREATE_VECTORDB_PROPS_MANDATORY, CREATE_VECTORDB_PROPS, props);
					String name = JsonUtil.getString(props, "name");
					VectorDB vectordb = broker.createVectordb(name);
					if(userIdentity != null) {
						String username = userIdentity.getUserPrincipal().getName();
						vectordb.setACL_owner(ACL.ofRole(username));
					}
					vectordbHandler_root.updateVectordbProps(vectordb, props, userIdentity, true);
					break;
				}
				default: 
					throw new RuntimeException("unknown key: "+key);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
