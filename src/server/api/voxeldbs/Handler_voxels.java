package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.TimeSlice;
import util.Web;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class Handler_voxels {
	private static final Logger log = LogManager.getLogger();

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String format = Web.getString(request, "format");

		double geoX = Web.getDouble(request, "x");
		double geoY = Web.getDouble(request, "y");
		double geoZ = Web.getDouble(request, "z");

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

		int rsize = 300;
		int rsize_d2 = rsize / 2;

		VoxelGeoRef ref = voxeldb.geoRef();
		int rx = ref.geoXtoVoxel(geoX);
		int ry = ref.geoYtoVoxel(geoY);
		int rz = ref.geoZtoVoxel(geoZ);
		int vrxmin = rx - rsize_d2;
		int vrymin = ry - rsize_d2;
		int vrzmin = rz;
		int vrxmax = vrxmin + rsize - 1;
		int vrymax = vrymin + rsize - 1;
		int vrzmax = vrzmin + rsize - 1;
		
		VoxelCntUint8Processor processor = new VoxelCntUint8Processor(voxeldb);
		processor.Process(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, timeSlice, response, format);
	}
}
