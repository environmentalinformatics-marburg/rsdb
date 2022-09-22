package remotetask.voxeldb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import voxeldb.VoxelDB;

@task_voxeldb("rename")
@Description("Renames an existing VoxelDB layer to a not yet existing VoxelDB layer ID")
@Param(name="voxeldb", type="voxeldb", desc="ID of VoxelDB layer to rename.", example="voxeldb1")
@Param(name="new_name", type="layer_id", desc="ID of new VoxelDB layer.", example="voxeldbA")
public class Task_rename extends RemoteTask {
	//

	private final Broker broker;
	private final JSONObject task;
	private final VoxelDB src;
	private final String dst;

	public Task_rename(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String voxeldb = task.getString("voxeldb");
		this.src = broker.getVoxeldb(voxeldb);
		src.checkMod(ctx.userIdentity);
		this.dst = task.getString("new_name");
	}

	@Override
	public void process() throws IOException {
		setMessage("rename VoxelDB '" + src.getName() + "' to '" + dst + "'");
		broker.renameVoxeldb(src.getName(), dst);
		setMessage("done, renamed VoxelDB '" + src.getName() + "' to '" + dst + "'");
	}
}
