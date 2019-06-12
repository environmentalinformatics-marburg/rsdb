package remotetask.rasterdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.Range2d;

@task_rasterdb("refresh_extent")
@Description("Recalculate extent of RasterDB layer")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer")
public class Task_refresh_extent extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB rasterdb;

	public Task_refresh_extent(Context ctx) {
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
			log.info("local range " + range);
		} catch(Exception e) {
			log.error(e);
		}
	}
}
