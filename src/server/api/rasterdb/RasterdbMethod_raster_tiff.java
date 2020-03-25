package server.api.rasterdb;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import rasterdb.RasterDB;

public class RasterdbMethod_raster_tiff extends RasterdbMethod {
	//private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_raster_tiff(Broker broker) {
		super(broker, "raster.tiff");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String queryString = request.getQueryString();
		//log.info("queryString " + queryString + "     " + queryString.endsWith(".xml"));
		if(queryString.endsWith(".xml")) { // workaround: Discard additional XML queries from GDAL.
			request.setHandled(true);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}		
		RequestProcessor.process("tiff", rasterdb, request, response);
	}

}