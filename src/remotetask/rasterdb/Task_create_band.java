package remotetask.rasterdb;

import org.json.JSONObject;

import broker.Broker;
import rasterdb.Band;
import rasterdb.Band.Builder;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_rasterdb("create_band")
@Description("Create new band in existing rasterdb layer.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="raster1")
@Param(name="band_number", type="integer", desc="Band ID, typically first band is number 1. If band_number is missing, next free band number will be set.", format="integer", example="1", required=false)
@Param(name="title", desc="Band title.", format="informal text", example="infrared", required=false)
@Param(name="type", type="integer", desc="Band data type: 1=tile_int16, 2=tile_float32, 3=INT16, 5=UINT16", example="1")
public class Task_create_band extends RemoteTask {

	private final Broker broker;
	private final JSONObject args;
	private final RasterDB rasterdb;

	public Task_create_band(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.args = ctx.task;
		String name = args.getString("rasterdb");
		this.rasterdb =  broker.getRasterdb(name);
		rasterdb.checkMod(ctx.userIdentity, "task rasterdb create_band of " + name);
	}

	@Override
	public void process() {				
		int type = args.getInt("type");
		String title = args.optString("title");		
		int band_number = args.optInt("band_number", Integer.MIN_VALUE);
		if(band_number == Integer.MIN_VALUE) {
			rasterdb.createBand(type, title, null);
		} else {
			Builder b = new Band.Builder(type, band_number);
			b.title = title;
			rasterdb.setBand(b.build(), false);
		}
	}
}
