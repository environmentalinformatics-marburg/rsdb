package remotetask.rasterdb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_rasterdb("rebuild_pyramid")
@Description("Recreate pyramid of scaled down rasters for visualisation.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="rasterdb1")
public class Task_rebuild_pyramid extends RemoteTask {
	
	private final Broker broker;
	private final JSONObject task;
	private final RasterDB rasterdb;
	
	public Task_rebuild_pyramid(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		this.rasterdb =  broker.getRasterdb(name);
		rasterdb.checkMod(ctx.userIdentity, "task rasterdb rebuild_pyramid");
	}

	@Override
	public void process() throws IOException {
		rasterdb.rebuildPyramid(true);		
	}
}
