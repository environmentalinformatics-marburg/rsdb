package server.api.pointdb;


import org.eclipse.jetty.server.Request;

import broker.Broker;
import pointdb.PointDB;
import server.api.APIHandler;
import util.Web;

public abstract class PointdbAPIHandler extends APIHandler {
	@SuppressWarnings("unused")
		

	public PointdbAPIHandler(Broker broker, String apiMethod) {
		super(broker, apiMethod);
	}

	protected PointDB getPointdb(Request request) {
		String name = request.getParameter("db");
		if(name == null) {
			throw new RuntimeException("missing db parameter for PointDB");
		}
		PointDB db = broker.getPointdb(name);
		if(db == null) {
			throw new RuntimeException("PointDB not found: " + name);
		}
		db.config.check(Web.getUserIdentity(request));
		return db;
	}
}
