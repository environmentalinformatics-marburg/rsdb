package remotetask.vectordb;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import vectordb.VectorDB;

@task_vectordb("refresh_catalog_entry")
@Description("update VectorDB layer catalog data")
@Param(name="vectordb", desc="ID of VectorDB layer")
public class RefreshCatalogEntryRemoteTask extends RemoteTask {

	private final Broker broker;
	private VectorDB vectordb;
	
	public RefreshCatalogEntryRemoteTask(Context ctx) {
		this.broker = ctx.broker;
		String name = ctx.task.getString("vectordb");
		this.vectordb =  broker.getVectorDB(name);
		vectordb.checkMod(ctx.userIdentity);
	}	

	@Override
	protected void process() throws Exception {
		setMessage("refresh catalog entry");
		broker.catalog.update(vectordb, true);
		setMessage("refresh catalog entry done.");
	}
}
