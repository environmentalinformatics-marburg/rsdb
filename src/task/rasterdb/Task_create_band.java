package task.rasterdb;

import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;

public class Task_create_band {
	
	private final Broker broker;
	private final JSONObject args;
	
	public Task_create_band(Broker broker, JSONObject args) {
		this.broker = broker;
		this.args = args;
	}

	public void run() {
		String name = args.getString("rasterdb");
		RasterDB rasterdb =  broker.getRasterdb(name);		
		int type = args.getInt("type");
		String title = args.optString("title");		
		rasterdb.createBand(type, title, null);		
	}
}
