package remotetask.pointcloud;


import broker.TimeSlice;
import pointcloud.PointCloud;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.pointdb.Abstract_task_index_raster;
import remotetask.pointdb.DataProvider2Factory;

@task_pointcloud("index_raster")
@Description("Create raster of pointcloud with index metrics calculations. Existing target RasterDB layer is needed.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="rasterdb", type="rasterdb", desc="Existing ID of RasterDB layer. (target)", example="rasterdb1")
@Param(name="indices", type="string_array", desc="List of indices.", example="BE_H_MAX, LAI, point_density")
@Param(name="rect", type="number_rect", desc="Extent to process.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="mask_band",  type="integer", desc="Band number of mask in RasterDB layer, no mask if left empty.", example="1", required=false)
@Param(name="time_slice", type="string", desc="Name of the pointcloud time slice. (default: latest)", example="January", required=false)
public class Task_index_raster extends Abstract_task_index_raster {
	

	public Task_index_raster(Context ctx) {
		super(ctx, getDpFactory(ctx));
	}

	private static DataProvider2Factory getDpFactory(Context ctx) {
		String name = ctx.task.getString("pointcloud");
		PointCloud pointcloud = ctx.broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity, "pointdb parameter of pointcloud index_raster");
		//EmptyACL.ADMIN.check(ctx.userIdentity); // modify rights are checked directly at target rasterdb
		int t = 0;
		String time_slice = ctx.task.optString("time_slice", null);
		if(time_slice != null) {
			TimeSlice timeSlice = pointcloud.getTimeSliceByName(time_slice);
			if(timeSlice == null) {
				throw new RuntimeException("time slice not found");
			} else {
				t = timeSlice.id;	
			}			
		} else if(!pointcloud.timeMapReadonly.isEmpty()) {
			t = pointcloud.timeMapReadonly.lastKey();
		}
		return new DataProvider2FactoryPointcloud(t, pointcloud);
	}	
}