package voxeldb.aggregator;

import voxeldb.aggregator.base.AggBool8ofInt32;

public class AggBool8ofInt32Exist extends AggBool8ofInt32 {	
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, boolean[][][] dst, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset, int factor_x, int factor_y, int factor_z) {
		for(int z = zSrcStart; z <= zSrcEnd; z++) {
			int[][] srcZ = src[z];
			boolean[][] dstZ = dst[(z + zSrcDstOffset) / factor_z];
			for(int y = ySrcStart; y <= ySrcEnd; y++) {
				int[] srcZY = srcZ[y];
				boolean[] dstZY = dstZ[(y + ySrcDstOffset) / factor_y];
				for(int x = xSrcStart; x <= xSrcEnd; x++) {
					if(srcZY[x] != 0) {
						dstZY[(x + xSrcDstOffset) / factor_x] = true;
					}
				}
			}
		}			
	}
}
