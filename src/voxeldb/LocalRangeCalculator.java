package voxeldb;

import java.io.IOException;

import rasterunit.KeyRange;
import rasterunit.TileKey;
import util.Range3d;

public class LocalRangeCalculator {

	private final VoxelDB voxeldb;
	private final CellFactory cellFactory;
	private Range3d cellRange;

	public LocalRangeCalculator(VoxelDB voxeldb) {
		this.voxeldb = voxeldb;
		this.cellFactory = new CellFactory(voxeldb).setCount();
	}

	public Range3d calc() throws IOException {
		this.cellRange = cellFactory.getCellRange();
		if(cellRange == null) {
			return null;
		}

		int cellSize = voxeldb.getCellsize();
		int cellMax = cellSize - 1;
		int lxmin = cellMax;
		int lymin = cellMax;
		int lzmin = cellMax;
		int lxmax = 0;
		int lymax = 0;
		int lzmax = 0;

		for(TileKey tileKey : voxeldb.getGriddb().getTileKeys()) {
			VoxelCell voxelCell = null;
			int x = tileKey.y;
			int y = tileKey.b;
			int z = tileKey.x;
			int t = tileKey.t;

			if(lxmin > 0 && cellRange.xmin == x) {
				if(voxelCell == null) {
					voxelCell = cellFactory.getVoxelCell(x, y, z, t);
				}
				lxmin = Math.min(lxmin, getXmin(voxelCell, lxmin));
			}
			if(lymin > 0 && cellRange.ymin == y) {
				if(voxelCell == null) {
					voxelCell = cellFactory.getVoxelCell(x, y, z, t);
				}
				lymin = Math.min(lymin, getYmin(voxelCell, lymin));
			}
			if(lzmin > 0 && cellRange.zmin == z) {
				if(voxelCell == null) {
					voxelCell = cellFactory.getVoxelCell(x, y, z, t);
				}
				lzmin = Math.min(lzmin, getZmin(voxelCell, lzmin));
			}			
			if(lxmax < cellMax && cellRange.xmax == x) {
				if(voxelCell == null) {
					voxelCell = cellFactory.getVoxelCell(x, y, z, t);
				}
				lxmax = Math.max(lxmax, getXmax(voxelCell, lxmax));
			}
			if(lymax < cellMax && cellRange.ymax == y) {
				if(voxelCell == null) {
					voxelCell = cellFactory.getVoxelCell(x, y, z, t);
				}
				lymax = Math.max(lymax, getYmax(voxelCell, lymax));
			}
			if(lzmax < cellMax && cellRange.zmax == z) {
				if(voxelCell == null) {
					voxelCell = cellFactory.getVoxelCell(x, y, z, t);
				}
				lzmax = Math.max(lzmax, getZmax(voxelCell, lzmax));
			}
		}
		return cellRange.mul(cellSize).add(lxmin, lymin, lzmin, lxmax, lymax, lzmax);
	}

	private int getXmin(VoxelCell voxelCell, int min) {
		int[][][] cube = voxelCell.cnt;
		int cellsize = voxeldb.getCellsize();
		for(int z = 0; z < cellsize; z++) {
			int[][] cubeZ = cube[z];
			for(int y = 0; y < cellsize; y++) {
				int[] cubeZY = cubeZ[y];
				for(int x = 0; x < min; x++) {
					if(cubeZY[x] > 0) {
						return x;
					}
				}
			}
		}
		return min;
	}

	private int getYmin(VoxelCell voxelCell, int min) {
		int[][][] cube = voxelCell.cnt;
		int cellsize = voxeldb.getCellsize();
		for(int z = 0; z < cellsize; z++) {
			int[][] cubeZ = cube[z];
			for(int y = 0; y < min; y++) {
				int[] cubeZY = cubeZ[y];
				for(int x = 0; x < cellsize; x++) {
					if(cubeZY[x] > 0) {
						return y;
					}
				}
			}
		}
		return min;
	}

	private int getZmin(VoxelCell voxelCell, int min) {
		int[][][] cube = voxelCell.cnt;
		int cellsize = voxeldb.getCellsize();
		for(int z = 0; z < min; z++) {
			int[][] cubeZ = cube[z];
			for(int y = 0; y < cellsize; y++) {
				int[] cubeZY = cubeZ[y];
				for(int x = 0; x < cellsize; x++) {
					if(cubeZY[x] > 0) {
						return z;
					}
				}
			}
		}
		return min;
	}

	private int getXmax(VoxelCell voxelCell, int max) {
		int[][][] cube = voxelCell.cnt;
		int cellsize = voxeldb.getCellsize();
		for(int z = 0; z < cellsize; z++) {
			int[][] cubeZ = cube[z];
			for(int y = 0; y < cellsize; y++) {
				int[] cubeZY = cubeZ[y];
				for(int x = cellsize - 1; x > max; x--) {
					if(cubeZY[x] > 0) {
						return x;
					}
				}
			}
		}
		return max;
	}

	private int getYmax(VoxelCell voxelCell, int max) {
		int[][][] cube = voxelCell.cnt;
		int cellsize = voxeldb.getCellsize();
		for(int z = 0; z < cellsize; z++) {
			int[][] cubeZ = cube[z];
			for(int y = cellsize - 1; y > max; y--) {
				int[] cubeZY = cubeZ[y];
				for(int x = 0; x < cellsize; x++) {
					if(cubeZY[x] > 0) {
						return y;
					}
				}
			}
		}
		return max;
	}

	private int getZmax(VoxelCell voxelCell, int max) {
		int[][][] cube = voxelCell.cnt;
		int cellsize = voxeldb.getCellsize();
		for(int z = cellsize - 1; z > max; z--) {
			int[][] cubeZ = cube[z];
			for(int y = 0; y < cellsize; y++) {
				int[] cubeZY = cubeZ[y];
				for(int x = 0; x < cellsize; x++) {
					if(cubeZY[x] > 0) {
						return z;
					}
				}
			}
		}
		return max;
	}
}
