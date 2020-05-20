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
@Description("Create visualisation raster of PointDB layer.")
@Param(name="pointdb", type="pointdb", desc="ID of PointDB layer. (source)", example="pointdb1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target)", example="rasterdb1")
@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) RasterDB operation mode. (RasterUnit only, default false)", example="false", required=false)
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
		RasterDB rasterdb;
		if(task.has("storage_type")) {
			String storage_type = task.getString("storage_type");
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions, storage_type);
		} else {
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions);	
		}		
		Rasterizer rasterizer = new Rasterizer(pointdb, rasterdb);
		rasterizer.run(rasterizer.bandIntensity);
		rasterizer.run(rasterizer.bandElevation);
		rasterdb.rebuildPyramid(true);
		rasterdb.flush();
	}
}
