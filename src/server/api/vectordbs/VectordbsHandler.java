package server.api.vectordbs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.EmptyACL;
import broker.catalog.CatalogKey;
import util.JsonUtil;
import util.Util;
import util.Web;
import vectordb.VectorDB;

public class VectordbsHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	public static final Marker MARKER_API = MarkerManager.getMarker("API");

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
	}

	private void addMethod(VectordbHandler handler) {
		addMethod(handler, handler.getMethod());
	}

	private void addMethod(VectordbHandler handler, String apiMethod) {
		String name = apiMethod;
		if (methodMap.containsKey(name)) {
			log.warn("method name already exists overwrite '" + name + "'  " + methodMap.get(name) + "  " + handler);
		}
		methodMap.put(name, handler);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Response baseResponse = (Response) response;
		baseRequest.setHandled(true);
		//log.info("vectordb request " + target);

		String currTarget = target.charAt(0) == '/' ? target.substring(1) : target;
		//log.info(MARKER_API, Web.getRequestLogString("API", currTarget, baseRequest));
		//log.info("currTarget " + currTarget);
		int nameSepIndex = currTarget.indexOf('/');
		String name = nameSepIndex < 0 ? currTarget : currTarget.substring(0, nameSepIndex); 
		//log.info("name " + name);
		String subTarget = nameSepIndex < 0 ? "" : currTarget.substring(nameSepIndex + 1);
		//log.info("subTarget " + subTarget);

		int methodSepIndex = subTarget.indexOf('/');
		String method = methodSepIndex < 0 ? subTarget : subTarget.substring(0, methodSepIndex); 
		//log.info("method " + method);
		String subsubTarget = methodSepIndex < 0 ? "" : subTarget.substring(methodSepIndex + 1);
		//log.info("subsubTarget " + subsubTarget);

		UserIdentity userIdentity = Web.getUserIdentity(baseRequest);
		//UserIdentity userIdentity = null;

		if(name.isEmpty()) {
			handleRoot(baseRequest, baseResponse, userIdentity);
			return;
		}
		VectordbHandler handler = methodMap.get(method);
		if(handler == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("unknown method " + method);
			return;
		}
		VectorDB vectordb;
		try {
			vectordb = broker.getVectorDB(name);
		} catch(Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println(e.getMessage());
			return;
		}		
		if(vectordb.isAllowed(userIdentity)) {
			try {
				handler.handle(vectordb, subsubTarget, baseRequest, (Response) response, userIdentity);
			} catch (Exception e) {
				log.error(e);
				try {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.setContentType("text/plain;charset=utf-8");
					response.getWriter().println(e.getMessage());
				} catch(Exception e1) {
					log.error(e1);
				}
			}
		} else {
			log.error("access not allowed for user");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("text/plain;charset=utf-8");
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
			log.error("HTTP method not allowed");
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("HTTP method not allowed");
			return;
		}	
	}

	public void handleRootGET(Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			boolean withDescription = request.getParameter("description") != null;
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
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
			log.error(e);
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
					//EmptyACL.ADMIN.check(userIdentity);
					JSONObject props = json.getJSONObject(key);
					Util.checkProps(CREATE_VECTORDB_PROPS_MANDATORY, CREATE_VECTORDB_PROPS, props);
					String name = JsonUtil.getString(props, "name");
					VectorDB vectordb = broker.createVectordb(name);
					vectordbHandler_root.updateVectordbProps(vectordb, props, userIdentity, true);
					break;
				}
				default: 
					throw new RuntimeException("unknown key: "+key);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
