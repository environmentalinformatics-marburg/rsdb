package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import util.Web;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class Handler_voxels {
	private static final Logger log = LogManager.getLogger();

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		
		double geoX = Web.getDouble(request, "x");
		double geoY = Web.getDouble(request, "y");
		double geoZ = Web.getDouble(request, "z");

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
		
		/*int vrxmin = (52634 - 2) * 50;
		int vrymin = (536219 - 2) * 50;
		int vrzmin = -1 * 50;
		int vrxmax = vrxmin + rsize - 1;
		int vrymax = vrymin + rsize - 1;
		int vrzmax = vrzmin + rsize - 1;*/
		
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		byte[][][] r = new byte[vrzlen][vrylen][vrxlen];
		

		int cellsize = voxeldb.getCellsize();
		int cellsize_m1 = cellsize - 1; 
		voxeldb.getVoxelCells().forEach(voxelCell -> {			
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
						rZY[x - vrxmin] = (byte) (v == 0 ? 0 : 1);
					}
				}
			}
		});
		

		byte[] data = new byte[vrxlen * vrylen * vrzlen];

		int pos = 0;
		for (int z = 0; z < vrzlen; z++) {
			byte[][] rZ = r[z];
			for (int y = 0; y < vrylen; y++) {
				byte[] rZY = rZ[y];
				for (int x = 0; x < vrxlen; x++) {
					byte v = rZY[x];
					data[pos++] = v;
				}
			}
		}


		/*VoxelCell voxelCell = voxeldb.getVoxelCell(52634, 536219, -1);
		log.info(voxelCell);

		int[][][] cnt = voxelCell.cnt;

		

		byte[] data = new byte[cellsize*cellsize*cellsize];

		int pos = 0;
		for (int z = 0; z < cellsize; z++) {
			int[][] cntZ = cnt[z];
			for (int y = 0; y < cellsize; y++) {
				int[] cntZY = cntZ[y];
				for (int x = 0; x < cellsize; x++) {
					int v = cntZY[x];
					data[pos++] = (byte) (v == 0 ? 0 : 1);
				}
			}
		}*/
		
		response.setContentType("application/octet-stream");
		response.getOutputStream().write(data);
	}

}
