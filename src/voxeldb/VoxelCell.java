package voxeldb;

public class VoxelCell {
	
	public final int x;
	public final int y;
	public final int z;
	public final int[][][] cnt;
	
	public VoxelCell(int x, int y, int z, int[][][] cnt) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cnt = cnt;
	}

	@Override
	public String toString() {
		return "VoxelCell [x=" + x + ", y=" + y + ", z=" + z + "]";
	}
}
