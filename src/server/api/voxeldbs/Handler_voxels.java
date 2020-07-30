package server.api.voxeldbs;

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import util.Web;
import util.rdat.Rdat;
import util.rdat.RdatList;
import voxeldb.TimeSlice;
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

		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		byte[][][] r = new byte[vrzlen][vrylen][vrxlen];		

		int cellsize = voxeldb.getCellsize();
		int cellsize_m1 = cellsize - 1; 
		voxeldb.getVoxelCells(timeSlice).forEach(voxelCell -> {			
			int vcxmin = voxelCell.x * cellsize;
			int vcymin = voxelCell.y * cellsize;
			int vczmin = voxelCell.z * cellsize;
			int vcxmax = vcxmin + cellsize_m1;
			int vcymax = vcymin + cellsize_m1;
			int vczmax = vczmin + cellsize_m1;			
			log.info(voxelCell);
			log.info("vc "+vcxmin+" "+vcymin+" "+vczmin+"   "+vcxmax+" "+vcymax+" "+vczmax);

			int vbxmin = Math.max(vrxmin, vcxmin);
			int vbymin = Math.max(vrymin, vcymin);
			int vbzmin = Math.max(vrzmin, vczmin);
			int vbxmax = Math.min(vrxmax, vcxmax);
			int vbymax = Math.min(vrymax, vcymax);
			int vbzmax = Math.min(vrzmax, vczmax);

			int[][][] cnt = voxelCell.cnt;
			for (int z = vbzmin; z <= vbzmax; z++) {
				int[][] cntZ = cnt[z - vczmin];
				byte[][] rZ = r[z - vrzmin];
				for (int y = vbymin; y <= vbymax; y++) {
					int[] cntZY = cntZ[y - vcymin];
					byte[] rZY = rZ[y - vrymin];
					for (int x = vbxmin; x <= vbxmax; x++) {
						int v = cntZY[x - vcxmin];
						//rZY[x - vrxmin] = (byte) (v == 0 ? 0 : 1);
						//rZY[x - vrxmin] = v > Byte.MAX_VALUE ? Byte.MAX_VALUE : (byte) v;
						//rZY[x - vrxmin] = (byte) v;
						rZY[x - vrxmin] = (byte) (v > 255 ? 255 : v);
					}
				}
			}
		});


		int clxmin = Integer.MAX_VALUE;
		int clymin = Integer.MAX_VALUE;
		int clzmin = Integer.MAX_VALUE;
		int clxmax = Integer.MIN_VALUE;
		int clymax = Integer.MIN_VALUE;
		int clzmax = Integer.MIN_VALUE;	
		for (int z = 0; z < vrzlen; z++) {
			byte[][] rZ = r[z];
			for (int y = 0; y < vrylen; y++) {
				byte[] rZY = rZ[y];
				for (int x = 0; x < vrxlen; x++) {
					byte v = rZY[x];
					if(v > 0) {
						if(x < clxmin) {
							clxmin = x;
						}
						if(clxmax < x) {
							clxmax = x;
						}
						if(y < clymin) {
							clymin = y;
						}
						if(clymax < y) {
							clymax = y;
						}
						if(z < clzmin) {
							clzmin = z;
						}
						if(clzmax < z) {
							clzmax = z;
						}						
					}
				}
			}
		}
		if(clymin == Integer.MAX_VALUE) {
			clxmin = 0;
			clymin = 0;
			clzmin = 0;
			clxmax = 0;
			clymax = 0;
			clzmax = 0;			
		}
		int clxlen = clxmax - clxmin + 1;
		int clylen = clymax - clymin + 1;
		int clzlen = clzmax - clzmin + 1;
		


		byte[] data = new byte[clxlen * clylen * clzlen];

		int pos = 0;
		for (int z = clzmin; z <= clzmax; z++) {
			byte[][] rZ = r[z];
			for (int y = clymin; y <= clymax; y++) {
				byte[] rZY = rZ[y];
				for (int x = clxmin; x <= clxmax; x++) {
					byte v = rZY[x];
					data[pos++] = v;
				}
			}
		}

		switch(format) {
		case "js": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.writeInt(clxlen);
			out.writeInt(clylen);
			out.writeInt(clzlen);	
			out.write(data);
			break;
		}
		case "rdat": {
			response.setContentType("application/octet-stream");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			out.write(Rdat.SIGNATURE_RDAT);
			out.write(Rdat.RDAT_TYPE_DIM_VECTOR);

			RdatList metaList = new RdatList();
			metaList.addString("name", voxeldb.getName());
			metaList.write(out);		

			Rdat.write_RDAT_VDIM_uint8(out, data, clxlen, clylen, clzlen);
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}




	}
}
