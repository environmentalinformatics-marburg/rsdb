package remotetask.pointcloud;

import java.io.IOException;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import broker.Informal.Builder;
import broker.TimeSlice;
import broker.acl.ACL;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.DoubleRect;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointcloud.TopFloatRaster;
import pointcloud.ZRasterFloat;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.Rect2i;
import rasterdb.node.ProcessorNode_gap_filling;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_pointcloud("surface_raster")
@Description("Create surface raster of PointCloud layer.  Currently DSM only.")
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
			DoubleRect rect = pointcloud.getRange();
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

		Band band = rasterdb.createBand(TilePixel.TYPE_FLOAT, "raster_surface", null);

		//final String rasterType = "DSM";
		//final String rasterType = "DTM";
		//final String rasterType = "CHM";
		final String rasterType = ctx.task.optString("surface", "DSM").toUpperCase();

		setMessage("process raster");

		Rect2i raster_rect = new Rect2i(raster_xmin, raster_ymin, raster_xmax, raster_ymax);
		int fill = 16;
		final int tile_size = 2048 - fill - fill;
		//final int tile_size = 1024;
		raster_rect.tiledIO(tile_size, tile_size, (xtile, ytile, xtilemax, ytilemax, xtmin, ytmin, xtmax, ytmax) -> {
			throwCanceled();
			setMessage("process raster tile " + (xtile+1) + ", " + (ytile+1) + " of " + (xtilemax+1) + ", " + (ytilemax+1));
			switch(rasterType) {
			case "DSM":
				processRasterDSM(pointcloud, rasterdb, timeSlice, band, xtmin, ytmin, xtmax, ytmax, false, fill);
				break;
			case "DTM":
				processRasterDTM(pointcloud, rasterdb, timeSlice, band, xtmin, ytmin, xtmax, ytmax, false, fill);
				break;	
			case "CHM":
				processRasterCHM(pointcloud, rasterdb, timeSlice, band, xtmin, ytmin, xtmax, ytmax, false, fill);
				break;				
			default:
				throw new RuntimeException("unknown raster type: " + rasterType);
			}

		});

		throwCanceled();
		setMessage("rebuild pyramid");
		rasterdb.rebuildPyramid(true);	
		setMessage("Done.");
	}

	private void processRasterDSM(PointCloud pointcloud, RasterDB rasterdb, TimeSlice timeSlice, Band band, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, boolean commit, int fill) throws IOException {
		GeoReference ref = rasterdb.ref();
		ref.throwNotSameXYpixelsize();
		double res = ref.pixel_size_x;
		double proc_xmin = ref.pixelXToGeo(raster_xmin - fill);
		double proc_ymin = ref.pixelYToGeo(raster_ymin - fill);
		double proc_xmax = ref.pixelXToGeoUpper(raster_xmax + fill);
		double proc_ymax = ref.pixelYToGeoUpper(raster_ymax + fill);
		int raster_width = raster_xmax - raster_xmin + 1;
		int raster_height = raster_ymax - raster_ymin + 1;
		int proc_raster_width = raster_width + fill + fill;
		int proc_raster_height = raster_height + fill + fill;
		setMessage("process raster "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);
		TopFloatRaster topFloatRaster_DSM = new TopFloatRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);

		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterEntity);
		pointTables.sequential().forEach(topFloatRaster_DSM::insert);
		if(topFloatRaster_DSM.getInsertedPointTablesCount() > 0) {
			float[][] raster_DSM = topFloatRaster_DSM.grid;
			if(fill > 0) {
				float[][] raster_DSM_dst = ProcessingFloat.copy(raster_DSM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DSM, raster_DSM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DSM = ProcessingFloat.withoutBorder(raster_DSM_dst, fill);			
			}

			RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
			int cnt = ProcessingFloat.writeMerge(rasterUnit, timeSlice.id, band, raster_DSM, raster_ymin, raster_xmin);
			if(commit) {
				rasterUnit.commit();
				setMessage("commited");
			}
			setMessage("tiles written: " + cnt);
		} else {
			setMessage("skip empty");
		}
	}

	private void processRasterDTM(PointCloud pointcloud, RasterDB rasterdb, TimeSlice timeSlice, Band band, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, boolean commit, int fill) throws IOException {
		GeoReference ref = rasterdb.ref();
		ref.throwNotSameXYpixelsize();
		double res = ref.pixel_size_x;
		double proc_xmin = ref.pixelXToGeo(raster_xmin - fill);
		double proc_ymin = ref.pixelYToGeo(raster_ymin - fill);
		double proc_xmax = ref.pixelXToGeoUpper(raster_xmax + fill);
		double proc_ymax = ref.pixelYToGeoUpper(raster_ymax + fill);
		int raster_width = raster_xmax - raster_xmin + 1;
		int raster_height = raster_ymax - raster_ymin + 1;
		int proc_raster_width = raster_width + fill + fill;
		int proc_raster_height = raster_height + fill + fill;
		setMessage("process raster "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);
		ZRasterFloat zRasterFloat_DTM = new ZRasterFloat(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);

		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterGround);
		pointTables.sequential().forEach(zRasterFloat_DTM::insert);
		if(zRasterFloat_DTM.getInsertedPointTablesCount() > 0) {
			float[][] raster_DTM = zRasterFloat_DTM.getMedian();
			if(fill > 0) {
				float[][] raster_DTM_dst = ProcessingFloat.copy(raster_DTM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DTM, raster_DTM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DTM = ProcessingFloat.withoutBorder(raster_DTM_dst, fill);			
			}

			RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
			int cnt = ProcessingFloat.writeMerge(rasterUnit, timeSlice.id, band, raster_DTM, raster_ymin, raster_xmin);
			if(commit) {
				rasterUnit.commit();
				setMessage("commited");
			}
			setMessage("tiles written: " + cnt);
		} else {
			setMessage("skip empty");
		}
	}
	
	private void processRasterCHM(PointCloud pointcloud, RasterDB rasterdb, TimeSlice timeSlice, Band band, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, boolean commit, int fill) throws IOException {
		GeoReference ref = rasterdb.ref();
		ref.throwNotSameXYpixelsize();
		double res = ref.pixel_size_x;
		double proc_xmin = ref.pixelXToGeo(raster_xmin - fill);
		double proc_ymin = ref.pixelYToGeo(raster_ymin - fill);
		double proc_xmax = ref.pixelXToGeoUpper(raster_xmax + fill);
		double proc_ymax = ref.pixelYToGeoUpper(raster_ymax + fill);
		int raster_width = raster_xmax - raster_xmin + 1;
		int raster_height = raster_ymax - raster_ymin + 1;
		int proc_raster_width = raster_width + fill + fill;
		int proc_raster_height = raster_height + fill + fill;
		setMessage("process raster "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);
		TopFloatRaster topFloatRaster_DSM = new TopFloatRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
		ZRasterFloat zRasterFloat_DTM = new ZRasterFloat(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);

		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterEntity);	
		pointTables.sequential().forEach(pointTable -> {
			topFloatRaster_DSM.insert(pointTable);
			zRasterFloat_DTM.insert(pointTable, pointTable.filterGround());	
		});		
		if(topFloatRaster_DSM.getInsertedPointTablesCount() > 0) {
			float[][] raster_DSM = topFloatRaster_DSM.grid;
			float[][] raster_DTM = zRasterFloat_DTM.getMedian();
			if(fill > 0) {
				float[][] raster_DSM_dst = ProcessingFloat.copy(raster_DSM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DSM, raster_DSM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DSM = ProcessingFloat.withoutBorder(raster_DSM_dst, fill);
				
				float[][] raster_DTM_dst = ProcessingFloat.copy(raster_DTM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DTM, raster_DTM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DTM = ProcessingFloat.withoutBorder(raster_DTM_dst, fill);			
			}
			float[][] raster_CHM = ProcessingFloat.minus(raster_DSM, raster_DTM, raster_DSM[0].length, raster_DSM.length);

			RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
			int cnt = ProcessingFloat.writeMerge(rasterUnit, timeSlice.id, band, raster_CHM, raster_ymin, raster_xmin);
			if(commit) {
				rasterUnit.commit();
				setMessage("commited");
			}
			setMessage("tiles written: " + cnt);
		} else {
			setMessage("skip empty");
		}		
	}
}
