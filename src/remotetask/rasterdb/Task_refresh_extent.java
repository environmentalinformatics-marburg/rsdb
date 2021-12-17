package remotetask.rasterdb;


import org.tinylog.Logger;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.Range2d;

@task_rasterdb("refresh_extent")
@Description("Recalculate extent of RasterDB layer. This may be needed if cached extent info is out of date.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="rasterdb1")
public class Task_refresh_extent extends RemoteTask {
	

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB rasterdb;

	public Task_refresh_extent(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		this.rasterdb =  broker.getRasterdb(name);
		rasterdb.check(ctx.userIdentity);
	}

	@Override
	public void process() {
		try {
			Range2d range = rasterdb.getLocalRange(true);
			Logger.info("local range " + range);
		} catch(Exception e) {
			Logger.error(e);
		}
	}
}
