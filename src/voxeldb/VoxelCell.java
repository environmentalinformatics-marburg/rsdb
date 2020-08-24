package voxeldb;

public class VoxelCell {
	
	public final int x;
	public final int y;
	public final int z;
	public final int[][][] cnt;
	public final int[][][] red;
	public final int[][][] green;
	public final int[][][] blue;
	
	public VoxelCell(int x, int y, int z, int[][][] cnt) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cnt = cnt;
		this.red = null;
		this.green = null;
		this.blue = null;
	}
	
	public VoxelCell(int x, int y, int z, int[][][] cnt, int[][][] red, int[][][] green, int[][][] blue) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cnt = cnt;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	@Override
	public String toString() {
		return "VoxelCell [x=" + x + ", y=" + y + ", z=" + z + "]";
	}

	public static VoxelCell ofFilled(int x, int y, int z, int xlen, int ylen, int zlen, boolean fillCnt, boolean fillRed, boolean fillGreen, boolean fillBlue) {
		int[][][] cnt = fillCnt ? new int[zlen][ylen][xlen] : null;
		int[][][] red = fillRed ? new int[zlen][ylen][xlen] : null;
		int[][][] green = fillGreen ? new int[zlen][ylen][xlen] : null;
		int[][][] blue = fillBlue ? new int[zlen][ylen][xlen] : null;
		VoxelCell voxelCell = new VoxelCell(x, y, z, cnt, red, green, blue);
		return voxelCell;
	}
	
	public static VoxelCell ofFilled(VoxelCell vc, int xlen, int ylen, int zlen, boolean fillCnt, boolean fillRed, boolean fillGreen, boolean fillBlue) {
		int[][][] cnt = vc.cnt == null ? (fillCnt ? new int[zlen][ylen][xlen] : null) : vc.cnt;
		int[][][] red = vc.red == null ? (fillRed ? new int[zlen][ylen][xlen] : null) : vc.red;
		int[][][] green = vc.green == null ?(fillGreen ? new int[zlen][ylen][xlen] : null) : vc.green;
		int[][][] blue = vc.blue == null ? (fillBlue ? new int[zlen][ylen][xlen] : null) : vc.blue;
		VoxelCell voxelCell = new VoxelCell(vc.x, vc.y, vc.z, cnt, red, green, blue);
		return voxelCell;
	}
	
	public int[][][] count() {
		return cnt;
	}
	
	public int[][][] red() {
		return red;
	}
	
	public int[][][] green() {
		return green;
	}
	
	public int[][][] blue() {
		return blue;
	}
}
