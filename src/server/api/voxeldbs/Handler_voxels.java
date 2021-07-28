package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.TimeSlice;
import server.api.voxeldbs.voxelcellprocessors.VcpCntUint16;
import server.api.voxeldbs.voxelcellprocessors.VcpCntUint8;
import server.api.voxeldbs.voxelcellprocessors.VcpInt32;
import server.api.voxeldbs.voxelcellprocessors.VcpInt32DivCount;
import util.Web;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class Handler_voxels {
	private static final Logger log = LogManager.getLogger();

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String format = Web.getString(request, "format");
		
		VoxelGeoRef ref = voxeldb.geoRef();
		
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
		
		String product = Web.getString(request, "product");

		//boolean clipped = Web.getFlag(request, "clipped");

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
		
		long cell_count_max = 100_000_000;
		long cell_count_x = (((long)vrxmax) - ((long)vrxmin) + 1);
		long cell_count_y = (((long)vrymax) - ((long)vrymin) + 1);
		long cell_count_z = (((long)vrzmax) - ((long)vrzmin) + 1);
		long cell_count =  cell_count_x * cell_count_y * cell_count_z;		
		log.info("cell_count: " + cell_count);
		if(cell_count_max < cell_count) {
			throw new RuntimeException("to large voxel subset requested, count of voxels: " + cell_count_x + "x" +  + cell_count_y + "x"  + cell_count_z + " = " + cell_count + "   max allowed: " + cell_count_max);
		}

		
		
		int cellsize = voxeldb.getCellsize();

		//String product = "cnt:uint8";

		switch(product) {
		case "count":
		case "count:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setCount());
			VcpInt32 vcp = new VcpInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::count);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "count:uint16": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setCount());
			VcpCntUint16 vcp = new VcpCntUint16(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
			processor.ProcessUint16(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "count:uint8": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setCount());
			VcpCntUint8 vcp = new VcpCntUint8(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
			processor.ProcessUint8(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "red":
		case "red:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setRed());
			VcpInt32 vcp = new VcpInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::red);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "green":
		case "green:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setGreen());
			VcpInt32 vcp = new VcpInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::green);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "blue":
		case "blue:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setBlue());
			VcpInt32 vcp = new VcpInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::blue);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "red_mean":
		case "red_mean:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setRed().setCount());
			VcpInt32 vcp = new VcpInt32DivCount(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::red);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "green_mean":
		case "green_mean:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setGreen().setCount());
			VcpInt32 vcp = new VcpInt32DivCount(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::green);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "blue_mean":
		case "blue_mean:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb, new CellFactory(voxeldb).setBlue().setCount());
			VcpInt32 vcp = new VcpInt32DivCount(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, VoxelCell::blue);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		default:
			throw new RuntimeException("unknown product");
		}

		
	}
}
