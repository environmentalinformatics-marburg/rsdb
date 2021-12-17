package server.api.rasterdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import rasterdb.RasterDB;

public abstract class RasterdbMethod {
	//
	
	protected static final String MIME_JSON = "application/json";

	protected final Broker broker;

	private String method;

	protected RasterdbMethod(Broker broker, String method) {
		this.broker = broker;
		this.method = method;
	}
	
	public abstract void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException;

	public String getMethod() {
		return method;
	}

}
