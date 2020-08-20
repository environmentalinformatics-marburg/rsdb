package server.api.voxeldbs.voxelcellprocessors;

import voxeldb.VoxelCell;

public class VcpCntInt32 extends VcpInt32 {

	public VcpCntInt32(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize) {
		super(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
	}
	
	@Override
	protected void process(VoxelCell voxelCell, int vbxmin, int vbymin, int vbzmin, int vbxmax, int vbymax, int vbzmax, int vcxmin, int vcymin, int vczmin) {
		int[][][] cnt = voxelCell.cnt;
		for (int z = vbzmin; z <= vbzmax; z++) {
			int[][] cntZ = cnt[z - vczmin];
			int[][] rZ = r[z - vrzmin];
			for (int y = vbymin; y <= vbymax; y++) {
				int[] cntZY = cntZ[y - vcymin];
				int[] rZY = rZ[y - vrymin];
				for (int x = vbxmin; x <= vbxmax; x++) {
					int v = cntZY[x - vcxmin];
					rZY[x - vrxmin] = v;
				}
			}
		}				
	}	
}