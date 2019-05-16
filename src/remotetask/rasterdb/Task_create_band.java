package remotetask.rasterdb;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.RemoteTask;

@task_rasterdb("create_band")
public class Task_create_band extends RemoteTask {
	
	private final Broker broker;
	private final JSONObject args;
	
	public Task_create_band(Broker broker, JSONObject args, UserIdentity userIdentity) {
		this.broker = broker;
		this.args = args;
	}

	@Override
	public void process() {
		String name = args.getString("rasterdb");
		RasterDB rasterdb =  broker.getRasterdb(name);		
		int type = args.getInt("type");
		String title = args.optString("title");		
		rasterdb.createBand(type, title, null);		
	}
}
