package server.api.main;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import util.JsonUtil;
import util.TimeUtil;

public class RemoveTimestampsRemoteTask extends RemoteTask {

	//private final Broker broker;
	//private final JSONObject args;
	private RasterDB rasterdb;
	private int[] timestamps;

	public RemoveTimestampsRemoteTask(Broker broker, JSONObject args, UserIdentity userIdentity) {
		//this.broker = broker;
		//this.args = args;
		String name = args.getString("rasterdb");
		this.rasterdb =  broker.getRasterdb(name);
		rasterdb.checkMod(userIdentity);
		this.timestamps = JsonUtil.getIntArray(args, "timestamps");
	}	

	@Override
	protected void process() throws Exception {
		setMessage("init remove timestamps");
		//Thread.sleep(10000);
		/*if(true) {
			throw new RuntimeException("my new error");
		}*/
		for(int timestamp: timestamps) {
			setMessage("remove timestamp " + timestamp + "  " + TimeUtil.toPrettyText(timestamp));			
			rasterdb.rasterUnit().removeTimestamp(timestamp);
			rasterdb.rasterPyr1Unit().removeTimestamp(timestamp);
			rasterdb.rasterPyr2Unit().removeTimestamp(timestamp);
			rasterdb.rasterPyr3Unit().removeTimestamp(timestamp);
			rasterdb.rasterPyr4Unit().removeTimestamp(timestamp);
			setMessage("remove timestamp " + timestamp + "  " + TimeUtil.toPrettyText(timestamp) + "  done.");
		}
		setMessage("remove timestamps done.");

	}

}
