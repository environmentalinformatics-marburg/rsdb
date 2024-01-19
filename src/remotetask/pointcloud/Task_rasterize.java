package remotetask.pointcloud;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.ACL;
import pointcloud.PointCloud;
import pointcloud.Rasterizer;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_pointcloud("rasterize")
@Description("Create visualisation raster of PointCloud layer.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target, default: [pointcloud]_rasterized) ", example="pointcloud1_rasterized", required=false)
@Param(name="processing_bands", type="string_array", desc="List of processing bands that should be processed. If needed point attributes are missing, a processing bands is omitted. Possible case sensitive values: red, green, blue, intensity, elevation (default all processings: red, green, blue, intensity, elevation)", example="intensity, elevation", required=false)
@Param(name="point_scale", type="number", desc="point coordinates to pixel scale factor (default: 4, results in 0.25 units pixel size)", example="4", required=false)
@Param(name="fill_radius", type="integer", desc="Fill pixel gaps up to fill_radius, high values slow down processing, with 0 => no filling (default: 3)", example="15", required=false)
@Param(name="associate", type="boolean", desc="Set the created raster layer as map visualisation for this point cloud layer. (default: true)", example="false", required=false)
//@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
//@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) RasterDB operation mode. (RasterUnit only, default false)", example="false", required=false)
public class Task_rasterize extends CancelableRemoteProxyTask {

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud pointcloud;
	private final String rasterdb_name;

	public Task_rasterize(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		pointcloud = broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity, "task pointcloud rasterize");
		pointcloud.checkMod(ctx.userIdentity, "task pointcloud rasterize"); // check needed as same ACLs are assigned to target rasterdb

		this.rasterdb_name = task.optString("rasterdb", pointcloud.getName() + "_rasterized");
		if(broker.hasRasterdb(rasterdb_name)) {
			RasterDB rasterdb = broker.getRasterdb(rasterdb_name);
			rasterdb.checkMod(ctx.userIdentity, "task pointcloud rasterize of existing name");
			rasterdb.close();
		}
	}

	@Override
	public void process() throws Exception {

		String[] processing_bands = null;
		JSONArray processing_bands_json = task.optJSONArray("processing_bands");
		if(processing_bands_json != null) {
			int processing_bands_json_len = processing_bands_json.length();
			processing_bands = new String[processing_bands_json_len];
			for (int i = 0; i < processing_bands_json_len; i++) {
				processing_bands[i] = processing_bands_json.getString(i);
			}
		}
		
		int fill_radius = task.optInt("fill_radius", 3);
		if(fill_radius < 0 || fill_radius > 100) {
			throw new RuntimeException("parameter fill_radius out of range of [0..100] :  " + fill_radius);
		}

		
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
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			rasterdb.setACL_owner(ACL.ofRole(username));
		}
		rasterdb.setACL(pointcloud.getACL());
		rasterdb.setACL_mod(pointcloud.getACL_mod());

		double point_scale = task.optDouble("point_scale", Rasterizer.DEFAULT_POINT_SCALE);

		pointcloud.Rasterizer rasterizer = new pointcloud.Rasterizer(pointcloud, rasterdb, point_scale, processing_bands, fill_radius);
		setRemoteProxyAndRunAndClose(rasterizer);
		if(isCanceled()) {
			throw new RuntimeException("canceled");
		}
		setMessage("rebuild pyramid");
		rasterdb.rebuildPyramid(true, this);		
		if(associate) {
			pointcloud.setAssociatedRasterDB(rasterdb_name);
		}
		rasterdb.setACL(pointcloud.getACL());
		rasterdb.setACL_mod(pointcloud.getACL_mod());
		setMessage("done");
	}
}
