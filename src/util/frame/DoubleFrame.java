package util.frame;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import util.Range2d;

public class DoubleFrame {
	//

	public final double[][] data;
	public final int width;
	public final int height;

	public final int local_min_x;
	public final int local_min_y;
	public final int local_max_x;
	public final int local_max_y;

	public final Map<String, Object> meta = new TreeMap<String, Object>();

	public DoubleFrame(double[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
	}

	public static DoubleFrame ofExtent(DoubleFrame extent) {
		return new DoubleFrame(new double[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static DoubleFrame ofExtent(ByteFrame extent) {
		return new DoubleFrame(new double[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	public static DoubleFrame ofRange2d(int width, int height, Range2d range2d) {
		return new DoubleFrame(new double[height][width], range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);		
	}

	/**
	 * create empty
	 * @param extent
	 * @return
	 */
	public static DoubleFrame ofExtent(ShortFrame extent) {
		return new DoubleFrame(new double[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	/**
	 * copy
	 * @param source
	 * @return
	 */
	public static DoubleFrame of(ShortFrame source) {
		DoubleFrame target = ofExtent(source);
		for (int y = 0; y < source.height; y++) {
			short[] s = source.data[y];
			double[] t = target.data[y];
			for (int x = 0; x < source.width; x++) {
				t[x] = s[x];
			}
		}
		return target;
	}

	public static DoubleFrame ofShortsWithNA(ShortFrame source, short na) {
		DoubleFrame target = ofExtent(source);
		for (int y = 0; y < source.height; y++) {
			short[] s = source.data[y];
			double[] t = target.data[y];
			for (int x = 0; x < source.width; x++) {
				short v = s[x];				
				t[x] = v == na ? Double.NaN : v;
			}
		}
		return target;
	}
	
	public static DoubleFrame ofBytesWithNA(ByteFrame source, byte na) {
		DoubleFrame target = ofExtent(source);
		byteToDouble(source.data, target.data, source.width, source.height, na);
		return target;
	}

	public static void byteToDouble(byte[][] src, double[][] dst, int width, int height, byte na) {
		for (int y = 0; y < height; y++) {
			byte[] s = src[y];
			double[] t = dst[y];
			for (int x = 0; x < width; x++) {
				byte v = s[x];				
				t[x] = v == na ? Double.NaN : v;
			}
		}
	}

	public static DoubleFrame minus(DoubleFrame a, DoubleFrame b) {
		DoubleFrame c = DoubleFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			double[] u = a.data[y];
			double[] v = b.data[y];
			double[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]-v[x];
			}
		}
		return c;
	}

	public static DoubleFrame mul(DoubleFrame a, double f) {
		DoubleFrame c = DoubleFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			double[] u = a.data[y];
			double[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]*f;
			}
		}
		return c;
	}

	public static DoubleFrame plus(DoubleFrame a, DoubleFrame b) {
		DoubleFrame c = DoubleFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			double[] u = a.data[y];
			double[] v = b.data[y];
			double[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]+v[x];
			}
		}
		return c;
	}

	public static DoubleFrame plus(DoubleFrame a, double v) {
		DoubleFrame c = DoubleFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			double[] u = a.data[y];
			double[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x] + v;
			}
		}
		return c;	
	}

	public static DoubleFrame div(DoubleFrame a, DoubleFrame b) {
		DoubleFrame c = DoubleFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			double[] u = a.data[y];
			double[] v = b.data[y];
			double[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]/v[x];
			}
		}
		return c;
	}

	public static DoubleFrame normalised_difference(DoubleFrame a, DoubleFrame b) {
		DoubleFrame c = DoubleFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			double[] u = a.data[y];
			double[] v = b.data[y];
			double[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = (u[x]-v[x])/(u[x]+v[x]);
			}
		}
		return c;
	}

	public DoubleFrame denoise() {
		DoubleFrame r = DoubleFrame.ofExtent(this);
		for (int y = 1; y < height-1; y++) {
			double[] row0 = data[y];
			double[] row1 = data[y];
			double[] row2 = data[y];
			double[] rrow = r.data[y];
			for (int x = 1; x < width-1; x++) {
				double mean = (row0[x-1]+row0[x]+row0[x+1]+row1[x-1]/*+row1[x]*/+row1[x+1]+row2[x-1]+row2[x]+row2[x+1])/9d;
				double dmean = mean-rrow[x];
				rrow[x] += dmean/2d; 
			}
		}
		return r;
	}

	public DoubleFrame addThis(DoubleFrame frame) {
		for (int y = 0; y < height; y++) {
			double[] t = data[y];
			double[] s = frame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] += s[x];
			}
		}
		return this;
	}

	public DoubleFrame substractThis(DoubleFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			double[] t = data[y];
			double[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] -= s[x];
			}
		}
		return this;
	}
	
	public DoubleFrame substractThis(double v) {
		for (int y = 0; y < height; y++) {
			double[] t = data[y];
			for (int x = 0; x < width; x++) {
				t[x] -= v;
			}
		}
		return this;
	}

	public DoubleFrame divThis(DoubleFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			double[] t = data[y];
			double[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] /= s[x];
			}
		}
		return this;
	}

	public DoubleFrame mulThis(DoubleFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			double[] t = data[y];
			double[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] *= s[x];
			}
		}
		return this;
	}
	
	public DoubleFrame powThis(DoubleFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			double[] t = data[y];
			double[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] = Math.pow(t[x], s[x]);
			}
		}
		return this;
	}

	public ShortFrame toFrame(double scaleFactor) {		
		int w = this.width;
		int h = this.height;		
		short[][] data = new short[h][w];
		ShortFrame r = new ShortFrame(data, this.local_min_x, this.local_min_y, this.local_max_x, this.local_max_y);

		double[][] src = this.data;
		for(int y=0;y<h;y++) {
			double[] src_row = src[y];
			short[] r_row = data[y];
			for(int x=0;x<w;x++) {
				r_row[x] = (short) (src_row[x] * scaleFactor);
			}
		}		
		return r;
	}

	public void fill(double constant) {
		for(double[] row:data) {
			Arrays.fill(row, constant);
		}

	}

	/**
	 * get range of values without zero-values.
	 * @return array with a[0] == min and a[1] == max
	 */
	public double[] getMinMax() {
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for(int y=0;y<height;y++) {
			double[] row = data[y];
			for(int x=0;x<width;x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					if(v<min) {
						min = v;
					}
					if(max<v) {
						max = v;
					}
				}
			}
		}
		return new double[]{min,max};
	}

	public static DoubleFrame euclidean_distance(DoubleFrame[] frames) {
		DoubleFrame result = DoubleFrame.ofExtent(frames[0]);
		double[][] resultData = result.data;
		int dims = frames.length;
		int width = result.width;
		int height = result.height;
		for(int d=0; d<dims; d++) {
			double[][] data = frames[d].data;
			for (int y = 0; y < height; y++) {
				double[] sourceRow = data[y];
				double[] resultRow = resultData[y];
				for (int x = 0; x < width; x++) {
					resultRow[x] += sourceRow[x] * sourceRow[x];
				}
			}
		}
		for (int y = 0; y < height; y++) {
			double[] resultRow = resultData[y];
			for (int x = 0; x < width; x++) {
				resultRow[x] = Math.sqrt(resultRow[x]);
			}
		}
		return result;
	}

	public static DoubleFrame ofFloats(float[][] data, Range2d range2d) {
		int w = data[0].length;
		int h = data.length;
		double[][] dst = new double[h][w];
		for (int y = 0; y < h; y++) {
			float[] s = data[y];
			double[] t = dst[y];
			for (int x = 0; x < w; x++) {
				t[x] = s[x];
			}
		}
		return new DoubleFrame(dst, range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);
	}

	public static DoubleFrame mean(DoubleFrame frameR, DoubleFrame frameB) {
		DoubleFrame frameG = DoubleFrame.ofExtent(frameR);
		double[][] dataR = frameR.data;
		double[][] dataG = frameG.data;
		double[][] dataB = frameB.data;
		int dWidth = frameG.width;
		int dHeight = frameG.height;
		for (int y = 0; y < dHeight; y++) {
			double[] rowR = dataR[y];
			double[] rowB = dataB[y];
			double[] rowG = dataG[y];
			for (int x = 0; x < dWidth; x++) {
				rowG[x] = (rowR[x] + rowB[x]) / 2d;
			}
		}
		return frameG;
	}
	
	public boolean hasNA() {
		for (int y = 0; y < height; y++) {
			double[] row = data[y];
			for (int x = 0; x < width; x++) {
				if(!Double.isFinite(row[x])) {
					return true;
				}
			}
		}
		return false;
	}

	public DoubleFrame copy() {
		DoubleFrame f = DoubleFrame.ofExtent(this);
		double[][] d = f.data;
		for (int i = 0; i < height; i++) {
		    System.arraycopy(data[i], 0, d[i], 0, width);
		}
		return f;
	}
}
