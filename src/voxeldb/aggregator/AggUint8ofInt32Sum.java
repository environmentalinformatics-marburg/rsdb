package voxeldb.aggregator;

import voxeldb.aggregator.base.AggUint8ofInt32;

public class AggUint8ofInt32Sum extends AggUint8ofInt32 {	
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, byte[][][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y, int factor_z) {
		if(factor_x == 1 && factor_y == 1 && factor_z == 1) {	
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				byte[][] dstZ = dst[z + zSrcDstOffeset];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					byte[] dstZY = dstZ[y + ySrcDstOffeset];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						int v = srcZY[x];
						dstZY[x + xSrcDstOffeset] = (byte) (v > 255 ? 255 : v);
					}
				}
			}
		} else {
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				byte[][] dstZ = dst[(z + zSrcDstOffeset) / factor_z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					byte[] dstZY = dstZ[(y + ySrcDstOffeset) / factor_y];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						int dstPosX = (x + xSrcDstOffeset) / factor_x;
						int v = srcZY[x] + Byte.toUnsignedInt(dstZY[dstPosX]);
						dstZY[dstPosX] =  (byte) (v > 255 ? 255 : v);
					}
				}
			}
		}
	}
}
