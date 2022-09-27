package remotetask.pointdb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.ACL;
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
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target, default: [pointdb]_rasterized) ", example="pointdb1_rasterized", required=false)
@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) RasterDB operation mode. (RasterUnit only, default false)", example="false", required=false)
public class Task_rasterize extends RemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final PointDB pointdb;
	private final String rasterdb_name;

	public Task_rasterize(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointdb");
		pointdb = broker.getPointdb(name);		
		pointdb.check(ctx.userIdentity, "task pointdb rasterize"); // check needed as same ACLs are assigned to target rasterdb, no ACL_mod in PointDB

		this.rasterdb_name = task.optString("rasterdb", pointdb.config.name + "_rasterized");
		if(broker.hasRasterdb(rasterdb_name)) {
			RasterDB rasterdb = broker.getRasterdb(rasterdb_name);
			rasterdb.checkMod(ctx.userIdentity, "task pointdb rasterize of existing name");
			rasterdb.close();
		}		
	}

	@Override
	public void process() throws IOException {
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
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			rasterdb.setACL_owner(ACL.of(username));
		}
		rasterdb.setACL(pointdb.getACL());
		rasterdb.setACL_mod(pointdb.getACL()); // no ACL_mod in PointDB
		Rasterizer rasterizer = new Rasterizer(pointdb, rasterdb);
		rasterizer.run(rasterizer.bandIntensity);
		rasterizer.run(rasterizer.bandElevation);
		rasterdb.rebuildPyramid(true);
		rasterdb.flush();
	}
}
