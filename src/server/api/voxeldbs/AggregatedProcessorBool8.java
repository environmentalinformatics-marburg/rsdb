package server.api.voxeldbs;

import voxeldb.VoxelCell;

public class AggregatedProcessorBool8 {
	public void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, boolean[][][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor) {
		int[][][] src = voxelCell.cnt;
		for(int z = zSrcStart; z <= zSrcEnd; z++) {
			int[][] srcZ = src[z];
			boolean[][] dstZ = dst[(z + zSrcDstOffeset) / factor];
			for(int y = ySrcStart; y <= ySrcEnd; y++) {
				int[] srcZY = srcZ[y];
				boolean[] dstZY = dstZ[(y + ySrcDstOffeset) / factor];
				for(int x = xSrcStart; x <= xSrcEnd; x++) {
					dstZY[(x + xSrcDstOffeset) / factor] = srcZY[x] != 0 ? true : dstZY[(x + xSrcDstOffeset) / factor];
				}
			}
		}		
	}
}