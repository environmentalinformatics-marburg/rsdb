package voxeldb.raster.agg;

public class RasterAggBool8ofInt32Exist extends RasterAggBool8ofInt32 {
	

	public final static RasterAggBool8ofInt32Exist DEFAULT = new RasterAggBool8ofInt32Exist();
	@Override
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, boolean[][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y) {
		if(factor_x == 1 && factor_y == 1) {			
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					boolean[] dstY = dst[y + ySrcDstOffeset];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstY[x + xSrcDstOffeset] |= srcZY[x] != 0;
					}
				}
			}	
		} else {
			for(int z = zSrcStart; z <= zSrcEnd; z++) {
				int[][] srcZ = src[z];
				for(int y = ySrcStart; y <= ySrcEnd; y++) {
					int[] srcZY = srcZ[y];
					boolean[] dstY = dst[(y + ySrcDstOffeset) / factor_y];
					for(int x = xSrcStart; x <= xSrcEnd; x++) {
						dstY[(x + xSrcDstOffeset) / factor_x] |= srcZY[x] != 0;
					}
				}
			}
		}
	}
}
