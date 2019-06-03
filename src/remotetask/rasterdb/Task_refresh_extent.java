package remotetask.rasterdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.RemoteTask;
import util.Range2d;

@task_rasterdb("refresh_extent")
public class Task_refresh_extent extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_refresh_extent(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
	}

	@Override
	public void process() {
		try {
			String name = task.getString("rasterdb");
			RasterDB rasterdb =  broker.getRasterdb(name);
			Range2d range = rasterdb.getLocalRange(true);
			log.info("local range " + range);
		} catch(Exception e) {
			log.error(e);
		}
	}
}
