package remotetask.rasterdb;

import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_rasterdb("create_band")
@Description("Create new band in existing rasterdb layer.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="raster1")
@Param(name="type", type="integer", desc="Band data type.", example="1")
@Param(name="title", desc="Band title.", format="informal text", example="infrared", required=false)
public class Task_create_band extends RemoteTask {
	
	private final Broker broker;
	private final JSONObject args;
	private final RasterDB rasterdb;
	
	public Task_create_band(Context ctx) {
		this.broker = ctx.broker;
		this.args = ctx.task;
		String name = args.getString("rasterdb");
		this.rasterdb =  broker.getRasterdb(name);
		rasterdb.checkMod(ctx.userIdentity);
	}

	@Override
	public void process() {				
		int type = args.getInt("type");
		String title = args.optString("title");		
		rasterdb.createBand(type, title, null);		
	}
}
