package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.TimeSlice;
import util.Range3d;
import util.Web;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class Handler_aggregated_voxels {
	private static final Logger log = LogManager.getLogger();

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();

		String format = Web.getString(request, "format");

		int vrxmin = Integer.MIN_VALUE;
		int vrymin = Integer.MIN_VALUE;
		int vrzmin = Integer.MIN_VALUE;
		int vrxmax = Integer.MIN_VALUE;
		int vrymax = Integer.MIN_VALUE;
		int vrzmax = Integer.MIN_VALUE;
		
		String extText = request.getParameter("ext");
		if(extText != null) {
			String[] ext = extText.split(" ");
			if(ext.length == 6) {
				double geoXmin = Double.parseDouble(ext[0]);
				double geoYmin = Double.parseDouble(ext[1]);
				double geoZmin = Double.parseDouble(ext[2]);
				double geoXmax = Double.parseDouble(ext[3]);
				double geoYmax = Double.parseDouble(ext[4]);
				double geoZmax = Double.parseDouble(ext[5]);
				
				vrxmin = ref.geoXtoVoxel(geoXmin);
				vrymin = ref.geoYtoVoxel(geoYmin);
				vrzmin = ref.geoZtoVoxel(geoZmin);
				vrxmax = ref.geoXtoVoxel(geoXmax);
				vrymax = ref.geoYtoVoxel(geoYmax);
				vrzmax = ref.geoZtoVoxel(geoZmax);
			} else if(ext.length == 4) {
				if(!Web.has(request, "zmin") || !Web.has(request, "zmax")) {
					throw new RuntimeException("parameter error in 'ext' for extent with (xmin, ymin, xmax, ymax) the addinaonal paramtets zmin, zmax are missing");
				}
				double geoXmin = Double.parseDouble(ext[0]);
				double geoYmin = Double.parseDouble(ext[1]);
				double geoZmin = Web.getDouble(request, "zmin");
				double geoXmax = Double.parseDouble(ext[2]);
				double geoYmax = Double.parseDouble(ext[3]);
				double geoZmax = Web.getDouble(request, "zmax");
				
				vrxmin = ref.geoXtoVoxel(geoXmin);
				vrymin = ref.geoYtoVoxel(geoYmin);
				vrzmin = ref.geoZtoVoxel(geoZmin);
				vrxmax = ref.geoXtoVoxel(geoXmax);
				vrymax = ref.geoYtoVoxel(geoYmax);
				vrzmax = ref.geoZtoVoxel(geoZmax);				
			} else {
				throw new RuntimeException("parameter error in 'ext' need to be 4 or 6 numbers (xmin, ymin, xmax, ymax) or (xmin, ymin, xmax, ymax, zmin, zmax): "+extText);
			}
			
		} else if(Web.has(request, "x") && Web.has(request, "y") && Web.has(request, "z")) {
			double geoX = Web.getDouble(request, "x");
			double geoY = Web.getDouble(request, "y");
			double geoZ = Web.getDouble(request, "z");
			
			int rsize = 300;
			int rsize_d2 = rsize / 2;

			int rx = ref.geoXtoVoxel(geoX);
			int ry = ref.geoYtoVoxel(geoY);
			int rz = ref.geoZtoVoxel(geoZ);
			vrxmin = rx - rsize_d2;
			vrymin = ry - rsize_d2;
			vrzmin = rz;
			vrxmax = vrxmin + rsize - 1;
			vrymax = vrymin + rsize - 1;
			vrzmax = vrzmin + rsize - 1;
		}
		
		if(Web.has(request, "xmin")) {
			double geoXmin = Web.getDouble(request, "xmin");
			vrxmin = ref.geoXtoVoxel(geoXmin);
		}
		if(Web.has(request, "xmax")) {
			double geoXmax = Web.getDouble(request, "xmax");
			vrxmax = ref.geoXtoVoxel(geoXmax);
		}
		
		if(Web.has(request, "ymin")) {
			double geoYmin = Web.getDouble(request, "ymin");
			vrymin = ref.geoYtoVoxel(geoYmin);
		}
		if(Web.has(request, "ymax")) {
			double geoYmax = Web.getDouble(request, "ymax");
			vrymax = ref.geoYtoVoxel(geoYmax);
		}
		
		if(Web.has(request, "zmin")) {
			double geoZmin = Web.getDouble(request, "zmin");
			vrzmin = ref.geoZtoVoxel(geoZmin);
		}
		if(Web.has(request, "zmax")) {
			double geoZmax = Web.getDouble(request, "zmax");
			vrzmax = ref.geoZtoVoxel(geoZmax);
		}
		
		if(vrxmin == Integer.MIN_VALUE) {
			throw new RuntimeException("missing xmin: needed parameter xmin or ext");
		}
		if(vrxmax == Integer.MIN_VALUE) {
			throw new RuntimeException("missing xmax: needed parameter xmax or ext");
		}
		if(vrymin == Integer.MIN_VALUE) {
			throw new RuntimeException("missing ymin: needed parameter ymin or ext");
		}
		if(vrymax == Integer.MIN_VALUE) {
			throw new RuntimeException("missing ymax: needed parameter ymax or ext");
		}
		if(vrzmin == Integer.MIN_VALUE) {
			throw new RuntimeException("missing zmin: needed parameter zmin or ext with 6 numbers");
		}
		if(vrzmax == Integer.MIN_VALUE) {
			throw new RuntimeException("missing zmax: needed parameter zmax or ext with 6 numbers");
		}
		
		Range3d range = new Range3d(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax);		

		int aggregation_factor_x = 1;
		int aggregation_factor_y = 1;
		int aggregation_factor_z = 1;		
		if(Web.has(request, "aggregation_factor")) {
			int aggregation_factor = Web.getInt(request, "aggregation_factor");
			aggregation_factor_x = aggregation_factor;
			aggregation_factor_y = aggregation_factor;
			aggregation_factor_z = aggregation_factor;
		}		
		if(Web.has(request, "aggregation_factor_x")) {
			aggregation_factor_x = Web.getInt(request, "aggregation_factor_x");
		}
		if(Web.has(request, "aggregation_factor_y")) {
			aggregation_factor_y = Web.getInt(request, "aggregation_factor_y");
		}
		if(Web.has(request, "aggregation_factor_z")) {
			aggregation_factor_z = Web.getInt(request, "aggregation_factor_z");
		}
		
		long cell_count_max = 100_000_000;
		long cell_count_x = (range.xmax - range.xmin) / aggregation_factor_x + 1;
		long cell_count_y = (range.ymax - range.ymin) / aggregation_factor_y + 1;
		long cell_count_z = (range.zmax - range.zmin) / aggregation_factor_z + 1;
		long cell_count =  cell_count_x * cell_count_y * cell_count_z;		
		log.info("aggregated cell_count: " + cell_count);
		if(cell_count_max < cell_count) {
			throw new RuntimeException("too large voxel subset requested, count of aggregated voxels: " + cell_count_x + "x" +  + cell_count_y + "x"  + cell_count_z + " = " + cell_count + "   max allowed: " + cell_count_max);
		}

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
		
		boolean crop = Web.getFlagBoolean(request, "crop");
		
		String product = Web.getString(request, "product", "sum");
		AggregatedProcessing.process(voxeldb, range, timeSlice, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, product, crop, response, format);		
	}
}
