package server.api.rasterdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import rasterdb.RasterDB;

public class RasterdbMethod_raster_rdat extends RasterdbMethod {

	public RasterdbMethod_raster_rdat(Broker broker) {
		super(broker, "raster.rdat");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		RequestProcessor.process("rdat", rasterdb, request, response);
	}

}