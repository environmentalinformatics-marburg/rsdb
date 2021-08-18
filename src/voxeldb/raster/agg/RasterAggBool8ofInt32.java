package voxeldb.raster.agg;

public abstract class RasterAggBool8ofInt32 {	
	public abstract void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, boolean[][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y);
}
