package remotetask.pointcloud;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.PointCloud;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import remotetask.pointdb.task_pointdb;

@task_pointcloud("rasterize")
@Description("create visualisation raster of PointCloud layer")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer (source)")
@Param(name="rasterdb", desc="ID of new RasterDB layer (target)")
@Param(name="transactions", desc="use power failer safe (and) slow RasterDB operation mode (default)", required=false)
public class Task_rasterize extends RemoteTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud pointcloud;

	public Task_rasterize(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		pointcloud = broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity);
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
		pointcloud.Rasterizer rasterizer = new pointcloud.Rasterizer(pointcloud, rasterdb);
		rasterizer.run();
		rasterdb.commit();
		rasterdb.rebuildPyramid();
	}
}
