package remotetask.vectordb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteProxyTask;
import vectordb.VectorDB;

@task_vectordb("rename")
@Description("Renames an existing VectorDB layer to a not yet existing VectorDB layer ID")
@Param(name="vectordb", type="layer_id", desc="ID of VectorDB layer to rename.", example="vectordb1")
@Param(name="new_name", type="layer_id", desc="ID of new VectorDB layer.", example="vectordbA")
public class Task_rename extends RemoteProxyTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final VectorDB src;
	private final String dst;

	public Task_rename(Context ctx) {
		EmptyACL.ADMIN.check(ctx.userIdentity);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String vectordb = task.getString("vectordb");
		this.src = broker.getVectorDB(vectordb);
		src.check(ctx.userIdentity);
		this.dst = task.getString("new_name");
	}

	@Override
	public void process() throws IOException {
		setMessage("rename VectorDB '" + src.getName() + "' to '" + dst + "'");
		broker.renameVectordb(src.getName(), dst);
		setMessage("done, renamed VectorDB '" + src.getName() + "' to '" + dst + "'");
	}
}
