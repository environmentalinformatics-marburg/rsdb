package remotetask.vectordb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import vectordb.VectorDB;

@task_vectordb("rename")
@Description("Renames an existing VectorDB layer to a not yet existing VectorDB layer ID")
@Param(name="vectordb", type="layer_id", desc="ID of VectorDB layer to rename.", example="vectordb1")
@Param(name="new_name", type="layer_id", desc="ID of new VectorDB layer.", example="vectordbA")
public class Task_rename extends RemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final VectorDB src;
	private final String dst;

	public Task_rename(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String vectordb = task.getString("vectordb");
		this.src = broker.getVectorDB(vectordb);
		src.checkMod(ctx.userIdentity, "task vectordb rename");
		this.dst = task.getString("new_name");
	}

	@Override
	public void process() throws IOException {
		setMessage("rename VectorDB '" + src.getName() + "' to '" + dst + "'");
		broker.renameVectordb(src.getName(), dst);
		setMessage("done, renamed VectorDB '" + src.getName() + "' to '" + dst + "'");
	}
}
