package remotetask.voxeldb;


import org.tinylog.Logger;
import org.json.JSONObject;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.Range3d;
import voxeldb.VoxelDB;

@task_voxeldb("refresh_extent")
@Description("Recalculate extent of VoxelDB layer. This may be needed if cached extent info is out of date.")
@Param(name="voxeldb", type="voxeldb", desc="VoxelDB layer.", example="voxeldb1")
public class Task_refresh_extent extends RemoteTask {
	

	private final Broker broker;
	private final JSONObject task;
	private final VoxelDB voxeldb;

	public Task_refresh_extent(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("voxeldb");
		this.voxeldb =  broker.getVoxeldb(name);
		voxeldb.check(ctx.userIdentity);   // no deep modify --> acl read is used
	}

	@Override
	public void process() {
		try {
			Range3d range = voxeldb.getLocalRange(true);
				setMessage("local range " + range);
		} catch(Exception e) {
			Logger.error(e);
		}
	}
}
