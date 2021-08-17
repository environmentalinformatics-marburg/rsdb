package voxeldb.aggregator.base;

public abstract class AggUint8ofInt32 {	
	public abstract void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, byte[][][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor_x, int factor_y, int factor_z);
}
