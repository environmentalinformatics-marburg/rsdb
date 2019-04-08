package server.api.main;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;
import vectordb.VectorDB;

public class RefreshCatalogEntryRemoteTask extends RemoteTask {

	private final Broker broker;
	private VectorDB vectordb;
	
	public RefreshCatalogEntryRemoteTask(Broker broker, JSONObject args, UserIdentity userIdentity) {
		this.broker = broker;
		String name = args.getString("vectordb");
		this.vectordb =  broker.getVectorDB(name);
		vectordb.checkMod(userIdentity);
	}	

	@Override
	protected void process() throws Exception {
		setMessage("refresh catalog entry");
		broker.catalog.update(vectordb, true);
		setMessage("refresh catalog entry done.");
	}

}
