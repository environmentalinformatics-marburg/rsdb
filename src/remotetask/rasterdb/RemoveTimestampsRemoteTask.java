package remotetask.rasterdb;

import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.RemoteTask;
import util.JsonUtil;
import util.TimeUtil;

@task_rasterdb("remove_timestamps")
public class RemoveTimestampsRemoteTask extends RemoteTask {

	//private final Broker broker;
	//private final JSONObject args;
	private RasterDB rasterdb;
	private int[] timestamps;

	public RemoveTimestampsRemoteTask(Context ctx) {
		//this.broker = broker;
		//this.args = args;
		String name = ctx.task.getString("rasterdb");
		this.rasterdb =  ctx.broker.getRasterdb(name);
		rasterdb.checkMod(ctx.userIdentity);
		this.timestamps = JsonUtil.getIntArray(ctx.task, "timestamps");
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
