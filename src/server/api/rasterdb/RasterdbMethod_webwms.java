package server.api.rasterdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.Broker;
import rasterdb.RasterDB;
import util.Web;

public class RasterdbMethod_webwms extends RasterdbMethod {

	public RasterdbMethod_webwms(Broker broker) {
		super(broker, "webwms");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			Logger.error("no WMS");
			return;
		}*/		

		String reqParam = Web.getLastString(request, "Request", null);
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "REQUEST", null);
		}
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "request", null);
		}
		if(reqParam==null) {
			reqParam = "GetCapabilities";
		}

		switch (reqParam) {
		case "GetMap":
			RasterdbMethod_webwms_GetMap.handle_GetMap(rasterdb, target, request, response, userIdentity);
			break;
		case "GetCapabilities":
			RasterdbMethod_webwms_GetCapabilities.handle_GetCapabilities(rasterdb, target, request, response, userIdentity);
			break;
		default:
			Logger.error("unknown request "+reqParam);
			return;
		}
	}	
}