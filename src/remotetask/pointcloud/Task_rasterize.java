package remotetask.pointcloud;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.PointCloud;
import pointcloud.Rasterizer;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_pointcloud("rasterize")
@Description("Create visualisation raster of PointCloud layer.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target, default: [pointcloud]_rasterized) ", example="pointcloud1_rasterized", required=false)
@Param(name="associate", type="boolean", desc="Set the created raster layer as map visualisation for this point cloud layer. (default: true)", example="false", required=false)
@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) RasterDB operation mode. (RasterUnit only, default false)", example="false", required=false)
@Param(name="point_scale", type="number", desc="point coordinates to pixel scale factor (default: 4, results in 0.25 units pixel size)", example="4", required=false)
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
		boolean associate = true;
		if(task.has("associate")) {
			associate = task.getBoolean("associate");
		}
		RasterDB rasterdb;
		if(task.has("storage_type")) {
			String storage_type = task.getString("storage_type");
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions, storage_type);
		} else {
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions);	
		}
		
		double point_scale = task.optDouble("point_scale", Rasterizer.DEFAULT_POINT_SCALE);
		
		pointcloud.Rasterizer rasterizer = new pointcloud.Rasterizer(pointcloud, rasterdb, point_scale);
		rasterizer.run();
		rasterdb.rebuildPyramid(true);
		if(associate) {
			pointcloud.setAssociatedRasterDB(rasterdb_name);
		}

	}
}
