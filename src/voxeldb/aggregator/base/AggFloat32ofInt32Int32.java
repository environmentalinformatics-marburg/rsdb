package voxeldb.aggregator.base;

public abstract class AggFloat32ofInt32Int32 {	
	public abstract void process(int[][][] srcA, int[][][] srcB, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, float[][][] dst, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset, int factor_x, int factor_y, int factor_z);
}
