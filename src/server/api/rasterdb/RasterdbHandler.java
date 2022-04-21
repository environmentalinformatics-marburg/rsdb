package server.api.rasterdb;

import java.io.IOException;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.Broker;
import broker.Broker.RasterDBNotFoundExeption;
import rasterdb.RasterDB;
import util.Web;

public class RasterdbHandler extends AbstractHandler {
	
	HashMap<String, RasterdbMethod> methodMap = new HashMap<String, RasterdbMethod>();

	private final Broker broker;

	public RasterdbHandler(Broker broker) {
		this.broker = broker;
		addMethod(new RasterdbMethod_meta_json(broker));
		addMethod(new RasterdbMethod_raster_rdat(broker));
		addMethod(new RasterdbMethod_raster_tiff(broker));
		addMethod(new RasterdbMethod_raster_png(broker));
		addMethod(new RasterdbMethod_raster_jpg(broker));
		addMethod(new RasterdbMethod_insert_raster(broker));
		addMethod(new RasterdbMethod_rebuild_pyramid(broker));
		addMethod(new RasterdbMethod_set(broker));
		addMethod(new RasterdbMethod_delete(broker));
		addMethod(new RasterdbMethod_pixel(broker));
		addMethod(new RasterdbMethod_packages(broker));
		addMethod(new RasterdbMethod_wms(broker));
		addMethod(new RasterdbMethod_wcs(broker));
		addMethod(new RasterdbMethod_attachments_zip(broker));
		addMethod(new RasterdbMethod_attachments(broker));
	}

	@SuppressWarnings("unused")
	private void addMethodAndAliases(RasterdbMethod handler, String... aliases) {
		addMethod(handler, handler.getMethod());
		for (String alias : aliases) {
			addMethod(handler, alias);
		}
	}

	private void addMethod(RasterdbMethod handler) {
		addMethod(handler, handler.getMethod());
	}

	private void addMethod(RasterdbMethod handler, String apiMethod) {
		String name = apiMethod;
		if (methodMap.containsKey(name)) {
			Logger.warn("method name already exists overwrite '" + name + "'  " + methodMap.get(name) + "  " + handler);
		}
		methodMap.put(name, handler);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		//Logger.info(MARKER_API, Web.getRequestLogString("API", target, baseRequest));
		//Logger.info("RasterDB request " + target);
		{
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
			if(name.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("missing rasterdb ");
				return;
			}
			if(method.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("RasterDB");
				return;
			}
			RasterdbMethod handler = methodMap.get(method);
			if(handler == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("unknown method " + method);
				return;
			}
			RasterDB rasterdb;
			try {
				rasterdb = broker.getRasterdb(name);
			} catch(RasterDBNotFoundExeption e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println(e.getMessage());
				return;
			}
			UserIdentity userIdentity = Web.getUserIdentity(baseRequest);
			if(rasterdb.isAllowed(userIdentity)) {
				try {
					handler.handle(rasterdb, subsubTarget, baseRequest, (Response) response, userIdentity);
				} catch (Exception e) {
					Logger.error(e);
					e.printStackTrace();
					try {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.setContentType("text/plain;charset=utf-8");
						response.getWriter().println(e.getMessage());
					} catch(Exception e1) {
						Logger.error(e1);
					}
				}
			} else {
				Logger.error("access not allowed for user");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("access not allowed for user");
			}
		}

		/*String method = target;
		if (method.charAt(0) == '/') {
			method = target.substring(1);
		}
		Logger.info(MARKER_API, Web.getRequestLogString("API", method, baseRequest));
		int sep = method.indexOf('/');
		if (sep > 0) {
			String name = method.substring(0, sep);
			String subMethod = method.substring(sep + 1);
			Logger.info("name " + name);
			Logger.info("subMethod " + subMethod);

			if (subMethod.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("PointDB");
				return;
			}
			if (subMethod.charAt(subMethod.length() - 1) == '/') {
				subMethod = subMethod.substring(0, subMethod.length() - 1);
			}
			// Logger.info("method: "+method);
			RasterdbMethod handler = methodMap.get(subMethod);
			if (handler == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("unknown method " + subMethod);
				return;
			}
			RasterDB rasterdb;
			try {
				rasterdb = broker.getRasterdb(name);
			} catch(RasterDBNotFoundExeption e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println(e.getMessage());
				return;
			}
			UserIdentity userIdentity = Web.getUserIdentity(baseRequest);
			if (rasterdb.isAllowed(userIdentity)) {
				try {
					handler.handle(rasterdb, baseRequest, (Response) response, userIdentity);
				} catch (Exception e) {
					Logger.error(e);
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.setContentType("text/plain;charset=utf-8");
					response.getWriter().println(e.getMessage());
				}
			} else {
				Logger.error("access not allowed for user");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("access not allowed for user");
			}
		}*/

	}
}
