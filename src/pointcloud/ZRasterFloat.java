package pointcloud;


import org.tinylog.Logger;

import com.googlecode.javaewah.datastructure.BitSet;

import rasterdb.tile.ProcessingFloat;
import util.collections.vec.FloatVec;

public class ZRasterFloat {

	public final FloatVec[][] grid;
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	public final double res;
	public final int xlen;
	public final int ylen;
	private int insertedPointTablesCount = 0;

	public ZRasterFloat(double xmin, double ymin, double xmax, double ymax, double res) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.res = res;
		this.xlen = (int) ((xmax - xmin) / res) + 1;
		this.ylen = (int) ((ymax - ymin) / res) + 1;
		this.grid = new FloatVec[ylen][xlen];
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
				float z = (float) zs[i];
				FloatVec list = grid[(int) y][(int) x];
				if(list == null) {
					list = new FloatVec();
					grid[(int) y][(int) x] = list;
				}
				list.add(z);
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
				float z = (float) zs[i];
				FloatVec list = grid[(int) y][(int) x];
				if(list == null) {
					list = new FloatVec();
					grid[(int) y][(int) x] = list;
				}
				list.add(z);
			}
		}
		insertedPointTablesCount++;
	}

	public float[][] getTop() {
		float[][] r = ProcessingFloat.createEmpty(xlen, ylen);		
		for (int y = 0; y < ylen; y++) {
			for (int x = 0; x < xlen; x++) {
				FloatVec c = grid[y][x];
				if(c != null) { // not empty
					float z = Float.NEGATIVE_INFINITY;
					int len = c.size;
					float[] buf = c.items;
					for(int i = 0; i < len; i++) {
						float bufz = buf[i];
						if(bufz > z) {
							z = bufz;
						}
					}
					r[y][x] = z;
				}

			}
		}
		return r;
	}

	public float[][] getMedian() {
		float[][] r = ProcessingFloat.createEmpty(xlen, ylen);		
		for (int y = 0; y < ylen; y++) {
			for (int x = 0; x < xlen; x++) {
				FloatVec c = grid[y][x];
				if(c != null) { // not empty
					int len = c.size();
					float[] buf = c.items;
					switch(len) {
					case 1:
						r[y][x] = buf[0];
						break;
					case 2:
						r[y][x] = (buf[0] + buf[1]) / 2f;	
						break;
					default:
						c.sort();
						if(len % 2 == 0) {
							int pos = len / 2;
							r[y][x] = (buf[pos - 1] + buf[pos]) / 2f;
						} else {
							r[y][x] =  buf[len / 2];
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
