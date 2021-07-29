package voxeldb.aggregator;

import voxeldb.aggregator.base.AggFloat32ofInt32Int32;

public abstract class AggFloat32ofInt32Int32DivSum extends AggFloat32ofInt32Int32 {	
	public void process(int[][][] srcA, int[][][] srcB, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, float[][][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y, int factor_z) {
	for(int z = zSrcStart; z <= zSrcEnd; z++) {
		int[][] srcAZ = srcA[z];
		int[][] srcBZ = srcB[z];
		float[][] dstZ = dst[(z + zSrcDstOffeset) / factor_z];
		for(int y = ySrcStart; y <= ySrcEnd; y++) {
			int[] srcAZY = srcAZ[y];
			int[] srcBZY = srcBZ[y];
			float[] dstZY = dstZ[(y + ySrcDstOffeset) / factor_y];
			for(int x = xSrcStart; x <= xSrcEnd; x++) {
				dstZY[(x + xSrcDstOffeset) / factor_x] += ((float)srcAZY[x]) / srcBZY[x];
			}
		}
	}		
}
}
