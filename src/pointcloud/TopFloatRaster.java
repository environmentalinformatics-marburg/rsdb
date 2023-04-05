package pointcloud;


import org.tinylog.Logger;

import com.googlecode.javaewah.datastructure.BitSet;

public class TopFloatRaster {

	public final float[][] grid;
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	public final double res;
	public final int xlen;
	public final int ylen;
	private int insertedPointTablesCount = 0;

	public TopFloatRaster(double xmin, double ymin, double xmax, double ymax, double res) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.res = res;
		this.xlen = (int) ((xmax - xmin) / res) + 1;
		this.ylen = (int) ((ymax - ymin) / res) + 1;
		this.grid = new float[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			float[] row = grid[y];
			for (int x = 0; x < xlen; x++) {
				row[x] = Float.NEGATIVE_INFINITY;
			}
		}
	}

	public void insert(PointTable pointTable) {
		int len = pointTable.rows;
		double[] xs = pointTable.x;
		double[] ys = pointTable.y;
		double[] zs = pointTable.z;
		for (int i = 0; i < len; i++) {
			try {
				double x = (xs[i] - xmin) / res;
				double y = (ys[i] - ymin) / res;
				double z = zs[i];
				if(grid[(int) y][(int) x] < z) {
					grid[(int) y][(int) x] = (float) z;
				}
			} catch(Exception e) {
				Logger.info("insert point " + xs[i] + " " + ys[i]);
				throw e;
			}
		}
		insertedPointTablesCount++;
	}

	public void insert(PointTable pointTable, BitSet filter) {
		int len = pointTable.rows;
		double[] xs = pointTable.x;
		double[] ys = pointTable.y;
		double[] zs = pointTable.z;
		for (int i = 0; i < len; i++) {
			if(filter.get(i)) {
				double x = (xs[i] - xmin) / res;
				double y = (ys[i] - ymin) / res;
				double z = zs[i];
				if(grid[(int) y][(int) x] < z) {
					grid[(int) y][(int) x] = (float) z;
				}
			}
		}
		insertedPointTablesCount++;
	}

	public int getInsertedPointTablesCount() {
		return insertedPointTablesCount;
	}
}
