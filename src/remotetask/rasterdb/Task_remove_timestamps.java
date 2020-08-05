package remotetask.rasterdb;

import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.JsonUtil;
import util.TimeUtil;

@task_rasterdb("remove_timestamps")
@Description("Remove all pixel data of all bands at some timestamps.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="rasterdb1")
@Param(name="timestamps", type="integer_array", desc="Array of integer timestamps.", example="1, 2, 3")
public class Task_remove_timestamps extends RemoteTask {

	//private final Broker broker;
	//private final JSONObject args;
	private RasterDB rasterdb;
	private int[] timestamps;

	public Task_remove_timestamps(Context ctx) {
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
			rasterdb.rasterUnit().removeAllTilesOfTimestamp(timestamp);
			rasterdb.rasterPyr1Unit().removeAllTilesOfTimestamp(timestamp);
			rasterdb.rasterPyr2Unit().removeAllTilesOfTimestamp(timestamp);
			rasterdb.rasterPyr3Unit().removeAllTilesOfTimestamp(timestamp);
			rasterdb.rasterPyr4Unit().removeAllTilesOfTimestamp(timestamp);
			rasterdb.removeTimeSlice(timestamp);
			setMessage("remove timestamp " + timestamp + "  " + TimeUtil.toPrettyText(timestamp) + "  done.");
		}
		setMessage("remove timestamps done.");

	}

}
