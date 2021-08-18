package voxeldb.aggregator.base;

public abstract class AggInt32ofInt32 {	
	public abstract void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int[][][] dst, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset, int factor_x, int factor_y, int factor_z);
}
