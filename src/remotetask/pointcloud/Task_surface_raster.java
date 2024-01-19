package remotetask.pointcloud;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import broker.Informal.Builder;
import broker.TimeSlice;
import broker.acl.ACL;
import pointcloud.Rect2d;
import pointcloud.PointCloud;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.Rect2i;
import rasterdb.tile.TilePixel;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import util.TaskPipeline;

@task_pointcloud("surface_raster")
@Description("Create surface raster of PointCloud layer.  DSM or DTM or CHM.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target, default: [pointcloud]_surface) ", example="pointcloud1_surface", required=false)
@Param(name="surface", type="string", desc="Raster surface type: DSM or DTM or CHM. (default: DSM)", example="DTM", required=false)
@Param(name="rect", type="number_rect", desc="Extent for surface_raster processing. (default: full pointcloud extent) ", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="time_slice", type="string", desc="Name of the pointcloud time slice. (default: latest)", example="January", required=false)
public class Task_surface_raster extends CancelableRemoteTask {

	private final Broker broker;
	private final JSONObject task;

	public Task_surface_raster(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
	}

	@Override
	protected void process() throws Exception {
		String name = ctx.task.getString("pointcloud");
		PointCloud pointcloud = ctx.broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity, "pointdb parameter of pointcloud surface_raster");

		JSONArray rect_Text = task.optJSONArray("rect");
		double surface_xmin;
		double surface_ymin;
		double surface_xmax;
		double surface_ymax;
		if(rect_Text != null) {
			surface_xmin = rect_Text.getDouble(0);
			surface_ymin = rect_Text.getDouble(1);
			surface_xmax = rect_Text.getDouble(2);
			surface_ymax = rect_Text.getDouble(3);
		} else {
			Rect2d rect = pointcloud.getRange();
			surface_xmin = rect.xmin;
			surface_ymin = rect.ymin;
			surface_xmax = rect.xmax;
			surface_ymax = rect.ymax;
		}

		double surface_res = 1d;

		String rasterdb_name = task.optString("rasterdb", pointcloud.getName() + "_surface");
		RasterDB rasterdb;
		if(broker.hasRasterdb(rasterdb_name)) {
			rasterdb = broker.getRasterdb(rasterdb_name);
			rasterdb.checkMod(ctx.userIdentity, "task pointcloud surface_raster of existing name");
		} else {
			rasterdb = broker.createNewRasterdb(rasterdb_name);	
			if(ctx.userIdentity != null) {
				String username = ctx.userIdentity.getUserPrincipal().getName();
				rasterdb.setACL_owner(ACL.ofRole(username));
			}
			rasterdb.setACL(pointcloud.getACL());
			rasterdb.setACL_mod(pointcloud.getACL_mod());			
		}

		rasterdb.setCode(pointcloud.getCode());
		rasterdb.setProj4(pointcloud.getProj4());
		rasterdb.associated.setPointCloud(pointcloud.getName());
		Builder informal = rasterdb.informal().toBuilder();
		informal.description = "surface_raster from poindcloud " + pointcloud.getName();
		rasterdb.setInformal(informal.build());
		rasterdb.writeMeta();

		TimeSlice ts = null;
		String time_slice_name = ctx.task.optString("time_slice", null);
		if(time_slice_name != null) {
			ts = pointcloud.getTimeSliceByName(time_slice_name);
			if(ts == null) {
				throw new RuntimeException("time slice not found");
			}		
		} else if(!pointcloud.timeMapReadonly.isEmpty()) {
			ts = pointcloud.timeMapReadonly.lastEntry().getValue();
		} else {
			ts = TimeSlice.ZERO_UNTITLED;
		}
		final TimeSlice timeSlice = ts;

		double pixel_size_x = surface_res;
		double pixel_size_y = surface_res;
		double offset_x = surface_xmin;
		double offset_y = surface_ymin;
		rasterdb.setPixelSize(pixel_size_x, pixel_size_y, offset_x, offset_y);
		final GeoReference ref = rasterdb.ref();

		int raster_xmin = ref.geoXToPixel(surface_xmin);
		int raster_ymin = ref.geoYToPixel(surface_ymin);
		int raster_xmax = ref.geoXToPixel(surface_xmax);
		int raster_ymax = ref.geoYToPixel(surface_ymax);

		if(!timeSlice.isZERO_UNTITLED()) {
			rasterdb.setTimeSlice(timeSlice);
		}

		//final String rasterType = "DSM";
		//final String rasterType = "DTM";
		//final String rasterType = "CHM";
		final String rasterType = ctx.task.optString("surface", "DSM").toUpperCase();

		Band band = rasterdb.createBand(TilePixel.TYPE_FLOAT, rasterType, null);

		setMessage("process raster");

		Rect2i raster_rect = new Rect2i(raster_xmin, raster_ymin, raster_xmax, raster_ymax);
		int fill = 16;
		final int tile_size = 2048 - fill - fill;
		//final int tile_size = 1024;

		TaskPipeline taskPipeline = new TaskPipeline(16);
		raster_rect.tiledThrows(tile_size, tile_size, (xtile, ytile, xtilemax, ytilemax, xtmin, ytmin, xtmax, ytmax) -> {
			throwCanceled();
			setMessage("process raster tile " + (xtile+1) + ", " + (ytile+1) + " of " + (xtilemax+1) + ", " + (ytilemax+1));			
			//processRaster(pointcloud, rasterdb, timeSlice, band, xtmin, ytmin, xtmax, ytmax, false, fill, rasterType);
			SurfaceRasterizer surfaceRasterizer = new SurfaceRasterizer(this, pointcloud, rasterdb, timeSlice, band, xtmin, ytmin, xtmax, ytmax, false, fill, rasterType);
			//surfaceRasterizer.process();
			//surfaceRasterizer.finish();
			taskPipeline.submit(surfaceRasterizer);

		});
		taskPipeline.join();

		throwCanceled();
		setMessage("rebuild pyramid");
		rasterdb.rebuildPyramid(true, this);	
		setMessage("Done.");
	}	
}
