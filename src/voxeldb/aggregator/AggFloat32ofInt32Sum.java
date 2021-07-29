package voxeldb.aggregator;

import voxeldb.aggregator.base.AggFloat32ofInt32;

public class AggFloat32ofInt32Sum extends AggFloat32ofInt32 {
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, float[][][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y, int factor_z) {
		if(factor_x == 1 && factor_y == 1 && factor_z == 1) {			
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				float[][] dstZ = dst[z + zSrcDstOffeset];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					float[] dstZY = dstZ[y + ySrcDstOffeset];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstZY[x + xSrcDstOffeset] = srcZY[x];
					}
				}
			}	
		} else {
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				float[][] dstZ = dst[(z + zSrcDstOffeset) / factor_z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					float[] dstZY = dstZ[(y + ySrcDstOffeset) / factor_y];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstZY[(x + xSrcDstOffeset) / factor_x] += srcZY[x];
					}
				}
			}
		}
	}
}
