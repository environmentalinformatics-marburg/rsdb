package voxeldb.voxelcellprocessors;

import java.util.function.Function;

import voxeldb.VoxelCell;

public class VcpInt32 extends Vcp {
	protected final Function<VoxelCell, int[][][]> mapper;
	
	protected int[][][] dst;

	public VcpInt32(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize, Function<VoxelCell, int[][][]> mapper) {
		super(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
		this.mapper = mapper;
	}

	public void setTarget(int[][][] target) {
		this.dst = target;
	}

	@Override
	protected void process(VoxelCell voxelCell, int vbxmin, int vbymin, int vbzmin, int vbxmax, int vbymax, int vbzmax, int vcxmin, int vcymin, int vczmin) {
		int[][][] src = mapper.apply(voxelCell);
		if(src != null) {
			for (int z = vbzmin; z <= vbzmax; z++) {
				int[][] srcZ = src[z - vczmin];
				int[][] dstZ = dst[z - vrzmin];
				for (int y = vbymin; y <= vbymax; y++) {
					int[] srcZY = srcZ[y - vcymin];
					int[] dstZY = dstZ[y - vrymin];
					for (int x = vbxmin; x <= vbxmax; x++) {
						int v = srcZY[x - vcxmin];
						dstZY[x - vrxmin] = v;
					}
				}
			}	
		}
	}
}