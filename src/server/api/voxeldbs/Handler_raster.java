package server.api.voxeldbs;

import java.io.IOException;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.TimeSlice;
import util.Range3d;
import util.Web;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;
import voxeldb.raster.RasterProc;
import voxeldb.raster.RasterProcBool8ofInt32;
import voxeldb.raster.RasterProcInt32ofInt32;
import voxeldb.raster.agg.RasterAggBool8ofInt32;
import voxeldb.raster.agg.RasterAggBool8ofInt32Exist;
import voxeldb.raster.agg.RasterAggInt32ofInt32;
import voxeldb.raster.agg.RasterAggInt32ofInt32Count;
import voxeldb.raster.agg.RasterAggInt32ofInt32Sum;
import voxeldb.voxelmapper.VoxelMapperInt32;

public class Handler_raster {	
	

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();
		String extText = Web.getString(request, "ext");
		String[] ext = extText.split(" ");
		if(ext.length != 4) {
			throw new RuntimeException("parameter error in 'ext': " + extText);
		}
		double req_xmin = Double.parseDouble(ext[0]);
		double req_ymin = Double.parseDouble(ext[1]);
		double req_xmax = Double.parseDouble(ext[2]);
		double req_ymax = Double.parseDouble(ext[3]);
		double req_zmin = Web.getDouble(request, "zmin", Double.NaN);
		double req_zmax = Web.getDouble(request, "zmax", Double.NaN);
		Logger.info("req "+req_xmin+" "+req_ymin+" "+req_xmax+" "+req_ymax + "    " + req_zmin + " " + req_zmax);
		Range3d range3d = ref.geoToRange(req_xmin, req_ymin, req_zmin, req_xmax, req_ymax, req_zmax);
		if(!Double.isFinite(req_zmin)) {
			range3d = range3d.withZmin(Integer.MIN_VALUE);
		}
		if(!Double.isFinite(req_zmax)) {
			range3d = range3d.withZmax(Integer.MAX_VALUE);
		}		
		Logger.info(range3d);
		
		TimeSlice timeSlice;
		if(Web.has(request, "time_slice_id")) {
			int time_slice_id = Web.getInt(request, "time_slice_id");
			timeSlice = voxeldb.timeMapReadonly.get(time_slice_id);
			if(timeSlice == null) {
				throw new RuntimeException("uknown time_slice_id: " + time_slice_id);
			}
			if(Web.has(request, "time_slice_name") && !Web.getString(request, "time_slice_name").equals(timeSlice.name)) {
				throw new RuntimeException("time_slice_name does not match to time slice of time_slice_id: '" + Web.getString(request, "time_slice_name") + "'  '" + timeSlice.name + "'");
			}
		} else if(Web.has(request, "time_slice_name")) {
			String time_slice_name = Web.getString(request, "time_slice_name");
			timeSlice = voxeldb.getTimeSliceByName(time_slice_name);
			if(timeSlice == null) {
				throw new RuntimeException("unknown time_slice_name: " + time_slice_name);
			}
		} else {
			if(voxeldb.timeMapReadonly.isEmpty()) {
				throw new RuntimeException("no data");
			}
			timeSlice = voxeldb.timeMapReadonly.lastEntry().getValue();
		}
		
		String product = Web.getString(request, "product", "sum");
		
		int aggregation_factor_x = 1;
		int aggregation_factor_y = 1;
		
		if(Web.has(request, "aggregation_factor")) {
			int aggregation_factor = Web.getInt(request, "aggregation_factor");
			aggregation_factor_x = aggregation_factor;
			aggregation_factor_y = aggregation_factor;
		}
		
		if(Web.has(request, "aggregation_factor_x")) {
			aggregation_factor_x = Web.getInt(request, "aggregation_factor_x");
		}
		
		if(Web.has(request, "aggregation_factor_y")) {
			aggregation_factor_y = Web.getInt(request, "aggregation_factor_y");
		}		
		
		String format = "rdat";
		boolean crop = false;
		process(voxeldb, range3d, timeSlice, aggregation_factor_x, aggregation_factor_y, response, product, format, crop);
	}

	private static void process(VoxelDB voxeldb, Range3d range, TimeSlice timeSlice, int aggregation_factor_x, int aggregation_factor_y, Response response, String product, String format, boolean crop) throws IOException {
		
		VoxelGeoRef ref = voxeldb.geoRef();
		double aggOriginX = ref.voxelXtoGeo(range.xmin);
		double aggOriginY = ref.voxelYtoGeo(range.ymin);
		double aggOriginZ = ref.voxelZtoGeo(range.zmin);
		double aggVoxelSizeX = ref.voxelSizeX * aggregation_factor_x;
		double aggVoxelSizeY = ref.voxelSizeY * aggregation_factor_y;
		Logger.info("res " + aggVoxelSizeX + " " + aggVoxelSizeY);
		VoxelGeoRef aggRef = ref.with(aggOriginX, aggOriginY, aggOriginZ, aggVoxelSizeX, aggVoxelSizeY, 0);
		
		CellFactory cellFactory = new CellFactory(voxeldb);
		RasterProc rasterProc = null;
		
		switch(product) {
		case "sum": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			RasterAggInt32ofInt32 aggregator = RasterAggInt32ofInt32Sum.DEFAULT;
			rasterProc = new RasterProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggRef, mapper, aggregator);			
			break;
		}
		case "count": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			RasterAggInt32ofInt32 aggregator = RasterAggInt32ofInt32Count.DEFAULT;
			rasterProc = new RasterProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggRef, mapper, aggregator);			
			break;
		}
		case "exist": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			RasterAggBool8ofInt32 aggregator = RasterAggBool8ofInt32Exist.DEFAULT;
			rasterProc = new RasterProcBool8ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggRef, mapper, aggregator);			
			break;
		}
		default:
			throw new RuntimeException("unknown product");
		}		
		
		cellFactory.getVoxelCells(timeSlice, range).forEach(rasterProc);
		rasterProc.finish();
		rasterProc.write(response, format, crop);
	}
}
