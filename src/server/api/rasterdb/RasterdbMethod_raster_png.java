package server.api.rasterdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import rasterdb.RasterDB;

public class RasterdbMethod_raster_png extends RasterdbMethod {
	//private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_raster_png(Broker broker) {
		super(broker, "raster.png");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		RequestProcessor.process("png", rasterdb, request, response);
	}

}