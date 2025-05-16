package remotetask.rasterdb;

import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.JsonUtil;

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
		rasterdb.checkMod(ctx.userIdentity, "task rasterdb remove bands");
		this.bands = JsonUtil.getIntArray(ctx.task, "bands");
	}	

	@Override
	protected void process() throws Exception {
		setMessage("init remove bands");
		for(int band: bands) {
			setMessage("remove band " + band);
			if(rasterdb.hasRasterUnit()) {
				rasterdb.rasterUnit().removeAllTilesOfBand(band);
			}
			if(rasterdb.hasRasterPyr1Unit()) {
				rasterdb.rasterPyr1Unit().removeAllTilesOfBand(band);
			}
			if(rasterdb.hasRasterPyr2Unit()) {
				rasterdb.rasterPyr2Unit().removeAllTilesOfBand(band);
			}
			if(rasterdb.hasRasterPyr3Unit()) {
				rasterdb.rasterPyr3Unit().removeAllTilesOfBand(band);
			}
			if(rasterdb.hasRasterPyr4Unit()) {
				rasterdb.rasterPyr4Unit().removeAllTilesOfBand(band);
			}
			rasterdb.removeBand(band);
			setMessage("remove band " + band + "  done.");
		}
		setMessage("remove bands done.");
	}

}
