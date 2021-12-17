package voxeldb.raster.agg;


import org.tinylog.Logger;

public class RasterAggInt32ofInt32Count extends RasterAggInt32ofInt32 {
	

	public final static RasterAggInt32ofInt32Count DEFAULT = new RasterAggInt32ofInt32Count();
	@Override
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int[][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y) {
		if(factor_x == 1 && factor_y == 1) {			
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					int[] dstY = dst[y + ySrcDstOffeset];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstY[x + xSrcDstOffeset] += srcZY[x] == 0 ? 0 : 1;
					}
				}
			}	
		} else {
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					int[] dstY = dst[(y + ySrcDstOffeset) / factor_y];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstY[(x + xSrcDstOffeset) / factor_x] += srcZY[x] == 0 ? 0 : 1;
					}
				}
			}
		}
	}
}
