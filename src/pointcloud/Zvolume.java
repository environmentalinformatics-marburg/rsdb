package pointcloud;

import util.collections.vec.DoubleVec;

public class Zvolume {
	public final DoubleVec[][] grid;
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	public final double res;
	
	public final DoubleVec zColl;
	
	public Zvolume(double xmin, double ymin, double xmax, double ymax, double res) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.res = res;
		this.grid = new DoubleVec[(int) ((ymax - ymin) / res) + 1][(int) ((xmax - xmin) / res) + 1];
		zColl = new DoubleVec();
	}
	
	public void insert(PointTable pointTable) {
		int len = pointTable.rows;
		double[] xs = pointTable.x;
		double[] ys = pointTable.y;
		double[] zs = pointTable.z;
		for (int i = 0; i < len; i++) {
			double x = (xs[i] - xmin) / res;
			double y = (ys[i] - ymin) / res;
			double z = zs[i];
			DoubleVec list = grid[(int) y][(int) x];
			if(list == null) {
				list = new DoubleVec();
				grid[(int) y][(int) x] = list;
			}
			list.add(z);
			this.zColl.add(z);
		}		
	}
	
	private double getZMedian() {
		int len = zColl.size();
		if(len == 0) {
			return 0d;
		}
		zColl.sort();
		if(len % 2 == 0) {
			int pos = len/2;
			return (zColl.get(pos-1) + zColl.get(pos)) / 2d;
		} else {
			return zColl.get(len/2);
		}
	}
	
	public double[] getZRange(double max_range) {
		double min = zColl.min();
	    double max = zColl.max();
	    if(max - min < max_range) {
	    	return new double[] {min, max};
	    } else {
	    	double median = getZMedian();
	    	double max_range_half = max_range / 2;
	    	return new double[] {median - min < max_range_half ? min : median - max_range_half, max - median < max_range_half ? max : median + max_range_half};
	    }
	}

	public int[][][] getVolume(double min, double max, double zres) {
		double low = Math.floor(min / zres);
		double high = Math.floor(max / zres);
		double m = low * zres;
		int range = ((int)(high - low)) + 1;
		int xrange = (int) ((xmax - xmin) / res) + 1;
		int yrange = (int) ((ymax - ymin) / res) + 1;
		int[][][] volume = new int[range][yrange][xrange];
		
		for (int y = 0; y < yrange; y++) {
			for (int x = 0; x < xrange; x++) {
				DoubleVec c = grid[y][x];
				if(c != null) {
					final int yy = y;
					final int xx = x;
					c.forEach(z->{
						int zz = (int) ((z - m) / zres);
						if(zz >= 0 && zz < range) {
							volume[zz][yy][xx]++;
						}
					});
				}

			}
		}
		
		return volume;
	}
}
