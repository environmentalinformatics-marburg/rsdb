package pointcloud;


import org.tinylog.Logger;

import com.googlecode.javaewah.datastructure.BitSet;

import rasterdb.tile.ProcessingDouble;
import rasterdb.tile.ProcessingFloat;
import util.collections.vec.Vec;

public class PointRaster {

	public final Vec<P3d>[][] grid;
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	public final double res;
	public final int xlen;
	public final int ylen;
	private int insertedPointTablesCount = 0;

	@SuppressWarnings("unchecked")
	public PointRaster(double xmin, double ymin, double xmax, double ymax, double res) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.res = res;
		this.xlen = (int) ((xmax - xmin) / res) + 1;
		this.ylen = (int) ((ymax - ymin) / res) + 1;
		this.grid = new Vec[ylen][xlen];
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
				Vec<P3d> list = grid[(int) y][(int) x];
				if(list == null) {
					list = new Vec<P3d>();
					grid[(int) y][(int) x] = list;
				}
				list.add(new P3d(x, y, z));
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
				Vec<P3d> list = grid[(int) y][(int) x];
				if(list == null) {
					list = new Vec<P3d>();
					grid[(int) y][(int) x] = list;
				}
				list.add(new P3d(x, y, z));
			}
		}
		insertedPointTablesCount++;
	}

	public double[][] getTop() {
		double[][] r = ProcessingDouble.createEmpty(xlen, ylen);		
		for (int y = 0; y < ylen; y++) {
			for (int x = 0; x < xlen; x++) {
				Vec<P3d> c = grid[y][x];
				if(c != null) { // not empty
					double z = Double.NEGATIVE_INFINITY;
					for(P3d p:c) {
						if(p.z > z) {
							z = p.z;
						}
					}
					r[y][x] = z;
				}

			}
		}
		return r;
	}
	
	public float[][] getTopFloat() {
		float[][] r = ProcessingFloat.createEmpty(xlen, ylen);		
		for (int y = 0; y < ylen; y++) {
			for (int x = 0; x < xlen; x++) {
				Vec<P3d> c = grid[y][x];
				if(c != null) { // not empty
					float z = Float.NEGATIVE_INFINITY;
					for(P3d p:c) {
						if(p.z > z) {
							z = (float) p.z;
						}
					}
					r[y][x] = z;
				}

			}
		}
		return r;
	}

	public double[][] getMedian() {
		double[][] r = ProcessingDouble.createEmpty(xlen, ylen);		
		for (int y = 0; y < ylen; y++) {
			for (int x = 0; x < xlen; x++) {
				Vec<P3d> c = grid[y][x];
				if(c != null) { // not empty
					int len = c.size();
					switch(len) {
					case 1:
						r[y][x] = c.get(0).z;
						break;
					case 2:
						r[y][x] = (c.get(0).z + c.get(1).z) / 2;	
						break;
					default:
						c.sort(P3d.Z_COMPARATOR);
						if(len % 2 == 0) {
							int pos = len / 2;
							r[y][x] = (c.get(pos - 1).z + c.get(pos).z) / 2d;
						} else {
							r[y][x] =  c.get(len / 2).z;
						}
					}
				}

			}
		}
		return r;
	}
	
	public int getInsertedPointTablesCount() {
		return insertedPointTablesCount;
	}
}
