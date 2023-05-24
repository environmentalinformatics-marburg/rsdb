package server.api.rasterdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.Broker;
import rasterdb.RasterDB;
import util.Web;

public class RasterdbMethod_wcs extends RasterdbMethod {
	
	public static final String NS_URL = "http://www.opengis.net/wcs";
	public static final String NS_XLINK = "http://www.w3.org/1999/xlink";
	public static final String NS_GML = "http://www.opengis.net/gml";	

	public RasterdbMethod_wcs(Broker broker) {
		super(broker, "wcs");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WCS".equals(request.getParameter("SERVICE"))) {
			Logger.error("no WCS");
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
			//reqParam = "GetCapabilities";
			reqParam = "DescribeCoverage";
		}

		switch (reqParam) {
		case "GetCapabilities":
			RasterdbMethod_wcs_GetCapabilities.handle_GetCapabilities(rasterdb, target, request, response, userIdentity);
			break;
		case "DescribeCoverage":			
			RasterdbMethod_wcs_DescribeCoverage.handle_DescribeCoverage(rasterdb, target, request, response, userIdentity);
			break;			
		case "GetCoverage":
			RasterdbMethod_wcs_GetCoverage.handle_GetCoverage(rasterdb, target, request, response, userIdentity);
			break;			
		default:
			Logger.error("unknown request " + reqParam);
			return;
		}
	}	
}