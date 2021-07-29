package voxeldb.voxelcellprocessors;

import java.util.function.Function;

import voxeldb.VoxelCell;

public class VcpInt32DivCount extends VcpInt32 {

	public VcpInt32DivCount(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize, Function<VoxelCell, int[][][]> mapper) {
		super(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, mapper);
	}

	@Override
	protected void process(VoxelCell voxelCell, int vbxmin, int vbymin, int vbzmin, int vbxmax, int vbymax, int vbzmax, int vcxmin, int vcymin, int vczmin) {
		int[][][] src = mapper.apply(voxelCell);
		int[][][] cnt = voxelCell.cnt;
		if(src != null && cnt != null) {
			for (int z = vbzmin; z <= vbzmax; z++) {
				int[][] srcZ = src[z - vczmin];
				int[][] cntZ = cnt[z - vczmin];
				int[][] dstZ = dst[z - vrzmin];
				for (int y = vbymin; y <= vbymax; y++) {
					int[] srcZY = srcZ[y - vcymin];
					int[] cntZY = cntZ[y - vcymin];
					int[] dstZY = dstZ[y - vrymin];
					for (int x = vbxmin; x <= vbxmax; x++) {
						int v = srcZY[x - vcxmin];
						int c = cntZY[x - vcxmin];
						dstZY[x - vrxmin] = c == 0 ? 0 : (v / c);
					}
				}
			}	
		}
	}
}