package remotetask.rasterdb;

import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.JsonUtil;
import util.TimeUtil;

@task_rasterdb("remove_bands")
@Description("Remove all pixel data of some bands at all timestamps.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="rasterdb1")
@Param(name="bands", type="integer_array", desc="Array of integer band numbers.", example="1, 2, 3")
public class Task_remove_bands extends RemoteTask {

	//private final Broker broker;
	//private final JSONObject args;
	private RasterDB rasterdb;
	private int[] bands;

	public Task_remove_bands(Context ctx) {
		super(ctx);
		//this.broker = broker;
		//this.args = args;
		String name = ctx.task.getString("rasterdb");
		this.rasterdb =  ctx.broker.getRasterdb(name);
		rasterdb.checkMod(ctx.userIdentity);
		this.bands = JsonUtil.getIntArray(ctx.task, "bands");
	}	

	@Override
	protected void process() throws Exception {
		setMessage("init remove bands");
		//Thread.sleep(10000);
		/*if(true) {
			throw new RuntimeException("my new error");
		}*/
		for(int band: bands) {
			setMessage("remove band " + band);
			rasterdb.removeBand(band);
			rasterdb.rasterUnit().removeAllTilesOfBand(band);
			rasterdb.rasterPyr1Unit().removeAllTilesOfBand(band);
			rasterdb.rasterPyr2Unit().removeAllTilesOfBand(band);
			rasterdb.rasterPyr3Unit().removeAllTilesOfBand(band);
			rasterdb.rasterPyr4Unit().removeAllTilesOfBand(band);
			setMessage("remove band " + band + "  " + TimeUtil.toPrettyText(band) + "  done.");
		}
		setMessage("remove bands done.");

	}

}
