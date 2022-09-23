package remotetask.vectordb;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import vectordb.VectorDB;

@task_vectordb("refresh_catalog_entry")
@Description("Update VectorDB layer catalog data.")
@Param(name="vectordb", type="vectordb", desc="ID of VectorDB layer.", example="vectordb1")
public class RefreshCatalogEntryRemoteTask extends RemoteTask {

	private final Broker broker;
	private VectorDB vectordb;
	
	public RefreshCatalogEntryRemoteTask(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		String name = ctx.task.getString("vectordb");
		this.vectordb =  broker.getVectorDB(name);
		vectordb.checkMod(ctx.userIdentity, "task vectordb refresh_catalog_entry");
	}	

	@Override
	protected void process() throws Exception {
		setMessage("refresh catalog entry");
		broker.catalog.update(vectordb, true);
		setMessage("refresh catalog entry done.");
	}
}
