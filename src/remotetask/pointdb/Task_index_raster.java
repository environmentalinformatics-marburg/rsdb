package remotetask.pointdb;


import pointdb.PointDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_pointdb("index_raster")
@Description("Create raster of pointcloud with index metrics calculations. Existing target RasterDB layer is needed.")
@Param(name="pointdb", type="pointdb", desc="ID of PointDB layer. (source)", example="pointdb1")
@Param(name="rasterdb", type="rasterdb", desc="Existing ID of RasterDB layer. (target)", example="rasterdb1")
@Param(name="indices", type="string_array", desc="List of indices.", example="BE_H_MAX, LAI, point_density")
@Param(name="rect", type="number_rect", desc="Extent to process.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="mask_band",  type="integer", desc="Band number of mask in RasterDB layer, no mask if left empty.", example="1", required=false)
public class Task_index_raster extends Abstract_task_index_raster {
	

	public Task_index_raster(Context ctx) {
		super(ctx, getDpFactory(ctx));
	}

	private static DataProvider2Factory getDpFactory(Context ctx) {
		String name = ctx.task.getString("pointdb");
		PointDB pointdb = ctx.broker.getPointdb(name);
		pointdb.config.getAcl().check(ctx.userIdentity, "pointdb parameter of pointdb index_raster");
		// EmptyACL.ADMIN.check(ctx.userIdentity); // modify rights are checked directly at target rasterdb
		return new DataProvider2FactoryPointdb(pointdb);
	}	
}