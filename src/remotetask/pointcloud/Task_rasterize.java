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

@task_pointcloud("rasterize")
@Description("create visualisation raster of PointCloud layer")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer (source)", example="pointcloud1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer (target, default: [pointcloud]_rasterized) ", example="pointcloud1_rasterized", required=false)
@Param(name="transactions", type="boolean", desc="use power failer safe (and) slow RasterDB operation mode (default) (obsolete for TileStorage)", example="false", required=false)
@Param(name="storage_type", desc="storage type of new RasterDB: RasterUnit (default) or TileStorage", format="RasterUnit or TileStorage", example="TileStorage", required=false)
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
		String rasterdb_name = task.optString("rasterdb", pointcloud.getName() + "_rasterized");
		boolean transactions = true;
		if(task.has("transactions")) {
			transactions = task.getBoolean("transactions");
		}
		RasterDB rasterdb;
		if(task.has("storage_type")) {
			String storage_type = task.getString("storage_type");
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions, storage_type);
		} else {
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions);	
		}
		pointcloud.Rasterizer rasterizer = new pointcloud.Rasterizer(pointcloud, rasterdb);
		rasterizer.run();
		rasterdb.rebuildPyramid();
		rasterdb.flush();
	}
}
