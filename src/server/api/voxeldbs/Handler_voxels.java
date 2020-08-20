package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.TimeSlice;
import server.api.voxeldbs.voxelcellprocessors.VcpCntInt32;
import server.api.voxeldbs.voxelcellprocessors.VcpCntUint8;
import util.Web;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class Handler_voxels {
	private static final Logger log = LogManager.getLogger();

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String format = Web.getString(request, "format");
		
		VoxelGeoRef ref = voxeldb.geoRef();
		
		int vrxmin;
		int vrymin;
		int vrzmin;
		int vrxmax;
		int vrymax;
		int vrzmax;
		
		String extText = request.getParameter("ext");
		if(extText != null) {
			String[] ext = extText.split(" ");
			if(ext.length != 6) {
				throw new RuntimeException("parameter error in 'ext': "+extText);
			}
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
		} else {
			throw new RuntimeException("missing extent: parameter 'ext' or parameters 'x' 'y' 'z'");
		}		
		
		String product = Web.getString(request, "product");

		//boolean clipped = Web.getFlag(request, "clipped");

		TimeSlice timeSlice;
		if(Web.has(request, "t")) {
			int t = Web.getInt(request, "t");
			timeSlice = voxeldb.timeMapReadonly.get(t);
			if(timeSlice == null) {
				throw new RuntimeException("uknown t: " + t);
			}
		} else {
			if(voxeldb.timeMapReadonly.isEmpty()) {
				throw new RuntimeException("no data");
			}
			timeSlice = voxeldb.timeMapReadonly.lastEntry().getValue();
		}

		
		
		int cellsize = voxeldb.getCellsize();

		//String product = "cnt:uint8";

		switch(product) {
		case "cnt":
		case "cnt:int32": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb);
			VcpCntInt32 vcp = new VcpCntInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
			processor.ProcessInt32(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		case "cnt:uint8": {
			VoxelProcessor processor = new VoxelProcessor(voxeldb);
			VcpCntUint8 vcp = new VcpCntUint8(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
			processor.ProcessUint8(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format, vcp);
			break;
		}
		default:
			throw new RuntimeException("unknown product");
		}

		
	}
}
