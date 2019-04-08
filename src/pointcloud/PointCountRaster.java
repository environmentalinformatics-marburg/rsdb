package pointcloud;

import java.util.BitSet;

public class PointCountRaster {

	public final int[][] grid;
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	public final double res;

	public PointCountRaster(double xmin, double ymin, double xmax, double ymax, double res) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.res = res;
		this.grid = new int[(int) ((xmax - xmin) / res) + 1][(int) ((ymax - ymin) / res) + 1];
	}

	public void insert(PointTable pointTable) {
		double[] xs = pointTable.x;
		double[] ys = pointTable.y;
		int len = pointTable.rows;
		for (int i = 0; i < len; i++) {
			double x = (xs[i] - xmin) / res;
			double y = (ys[i] - ymin) / res;
			grid[(int) y][(int) x]++;
		}

	}

	public void insert(PointTable pointTable, BitSet filter) {
		double[] xs = pointTable.x;
		double[] ys = pointTable.y;		
		int len = pointTable.rows;
		for (int i = 0; i < len; i++) {
			if(filter.get(i)) {
				double x = (xs[i] - xmin) / res;
				double y = (ys[i] - ymin) / res;
				grid[(int) y][(int) x]++;
			}
		}

	}

}
