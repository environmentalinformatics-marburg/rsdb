package task.rasterdb;

import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;

public class Task_rebuild_pyramid {
	//private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final JSONObject task;
	
	public Task_rebuild_pyramid(Broker broker, JSONObject task) {
		this.broker = broker;
		this.task = task;
	}

	public void run() {
		String name = task.getString("rasterdb");
		RasterDB rasterdb =  broker.getRasterdb(name);
		rasterdb.rebuildPyramid();		
	}

}
