package voxeldb.aggregator;

import voxeldb.aggregator.base.AggInt32ofInt32;

public class AggInt32ofInt32Sum extends AggInt32ofInt32 {	
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int[][][] dst, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset, int factor_x, int factor_y, int factor_z) {
		if(factor_x == 1 && factor_y == 1 && factor_z == 1) {			
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				int[][] dstZ = dst[z + zSrcDstOffset];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					int[] dstZY = dstZ[y + ySrcDstOffset];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstZY[x + xSrcDstOffset] = srcZY[x];
					}
				}
			}	
		} else {
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				int[][] dstZ = dst[(z + zSrcDstOffset) / factor_z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					int[] dstZY = dstZ[(y + ySrcDstOffset) / factor_y];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstZY[(x + xSrcDstOffset) / factor_x] += srcZY[x];
					}
				}
			}
		}
	}
}
