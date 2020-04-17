package server.api.rasterdb;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import rasterdb.RasterDB;

public class RasterdbMethod_raster_tiff extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_raster_tiff(Broker broker) {
		super(broker, "raster.tiff");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		
		String queryString = request.getQueryString();
		log.info("queryString " + queryString + "     " + queryString.endsWith(".xml"));
		Enumeration<String> it = request.getHeaderNames();
		while(it.hasMoreElements()) {
			String e = it.nextElement();
			log.info("   " + e + "  " + request.getHeader(e));			
		}
		
		if(request.getHeader("range") != null) {
			log.info("filtered range request");
			request.setHandled(true);
			response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			response.getWriter().println("HTTP range request not supported");
			return;
		}
		
		if(queryString.endsWith(".xml") 
				|| queryString.endsWith(".XML")
				|| queryString.endsWith(".txt") 
				|| queryString.endsWith(".TXT") 
				|| queryString.endsWith(".pass") 
				|| queryString.endsWith(".PASS")
				|| queryString.endsWith(".aux")
				|| queryString.endsWith(".AUX")
				|| queryString.endsWith(".ovr")
				|| queryString.endsWith(".msk")
				|| queryString.endsWith(".rsc")				
				|| queryString.endsWith(".IMD")
				|| queryString.endsWith(".imd")
				|| queryString.endsWith(".rpb")
				|| queryString.endsWith(".RPB")
				|| queryString.endsWith(".rpc")
				|| queryString.endsWith(".RPC")
				|| queryString.endsWith(".PVL")
				|| queryString.endsWith(".pvl")) { // workaround: Discard additional XML queries from GDAL.
			log.info("filtered   " + queryString);
			request.setHandled(true);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}		
		
		String httpMethod = request.getMethod();
		switch(httpMethod) {
		case "GET": {			
			RequestProcessor.process("tiff", rasterdb, request, response);
			break;
		}
		case "HEAD": {
			RequestProcessor.process("tiff", rasterdb, request, response);
			break;
			/*response.setStatus(HttpServletResponse.SC_OK);
			request.setHandled(true);
			break;*/
		}
		default:
			log.warn("unknown method: " + httpMethod);
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			request.setHandled(true);
		}
	}

}