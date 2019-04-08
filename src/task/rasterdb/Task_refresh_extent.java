package task.rasterdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import util.Range2d;

public class Task_refresh_extent {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_refresh_extent(Broker broker, JSONObject task) {
		this.broker = broker;
		this.task = task;
	}

	public void run() {
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
