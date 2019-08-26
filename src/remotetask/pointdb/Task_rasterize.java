package remotetask.pointdb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointdb.PointDB;
import pointdb.Rasterizer;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_pointdb("rasterize")
@Description("create visualisation raster of PointDB layer")
@Param(name="pointdb", type="pointdb", desc="ID of PointDB layer (source)")
@Param(name="rasterdb", desc="ID of new RasterDB layer (target)")
@Param(name="transactions", desc="use power failer safe (and) slow RasterDB operation mode (default)", required=false)
public class Task_rasterize extends RemoteTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointDB pointdb;

	public Task_rasterize(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointdb");
		pointdb = broker.getPointdb(name);
		pointdb.config.getAcl().check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() throws IOException {
		String rasterdb_name = task.getString("rasterdb");
		boolean transactions = true;
		if(task.has("transactions")) {
			transactions = task.getBoolean("transactions");
		}		
		RasterDB rasterdb = broker.createRasterdb(rasterdb_name, transactions);		
		Rasterizer rasterizer = new Rasterizer(pointdb, rasterdb);
		rasterizer.run(rasterizer.bandIntensity);
		rasterizer.run(rasterizer.bandElevation);
		rasterdb.commit();
		rasterdb.rebuildPyramid();
	}
}
