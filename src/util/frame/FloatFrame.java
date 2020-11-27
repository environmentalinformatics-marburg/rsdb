package util.frame;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Range2d;

public class FloatFrame {
	private static final Logger log = LogManager.getLogger();

	public final float[][] data;
	public final int width;
	public final int height;

	public final int local_min_x;
	public final int local_min_y;
	public final int local_max_x;
	public final int local_max_y;

	public final Map<String, Object> meta = new TreeMap<String, Object>();

	public FloatFrame(float[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
	}

	public static FloatFrame of(float[][] data, Range2d range2d) {
		return new FloatFrame(data, range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);	
	}

	public static FloatFrame ofExtent(FloatFrame extent) {
		return new FloatFrame(new float[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static FloatFrame ofExtent(ByteFrame extent) {
		return new FloatFrame(new float[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	public static FloatFrame ofRange2d(Range2d r, Range2d range2d) {
		return new FloatFrame(new float[r.getHeight()][r.getWidth()], range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);		
	}

	/**
	 * create empty
	 * @param extent
	 * @return
	 */
	public static FloatFrame ofExtent(ShortFrame extent) {
		return new FloatFrame(new float[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	/**
	 * copy
	 * @param source
	 * @return
	 */
	public static FloatFrame of(ShortFrame source) {
		FloatFrame target = ofExtent(source);
		for (int y = 0; y < source.height; y++) {
			short[] s = source.data[y];
			float[] t = target.data[y];
			for (int x = 0; x < source.width; x++) {
				t[x] = s[x];
			}
		}
		return target;
	}

	public static FloatFrame ofShortsWithNA(ShortFrame source, short na) {
		FloatFrame target = ofExtent(source);
		shortToFloat(source.data, target.data, source.width, source.height, na);
		return target;
	}

	public static void shortToFloat(short[][] src, float[][] dst, int width, int height, short na) {
		for (int y = 0; y < height; y++) {
			short[] s = src[y];
			float[] t = dst[y];
			for (int x = 0; x < width; x++) {
				short v = s[x];				
				t[x] = v == na ? Float.NaN : v;
			}
		}
	}
	
	public static FloatFrame ofBytesWithNA(ByteFrame source, byte na) {
		FloatFrame target = ofExtent(source);
		byteToFloat(source.data, target.data, source.width, source.height, na);
		return target;
	}

	public static void byteToFloat(byte[][] src, float[][] dst, int width, int height, byte na) {
		for (int y = 0; y < height; y++) {
			byte[] s = src[y];
			float[] t = dst[y];
			for (int x = 0; x < width; x++) {
				byte v = s[x];				
				t[x] = v == na ? Float.NaN : v;
			}
		}
	}

	public static FloatFrame minus(FloatFrame a, FloatFrame b) {
		FloatFrame c = FloatFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			float[] u = a.data[y];
			float[] v = b.data[y];
			float[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]-v[x];
			}
		}
		return c;
	}

	public static FloatFrame mul(FloatFrame a, float f) {
		FloatFrame c = FloatFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			float[] u = a.data[y];
			float[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]*f;
			}
		}
		return c;
	}

	public static FloatFrame plus(FloatFrame a, FloatFrame b) {
		FloatFrame c = FloatFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			float[] u = a.data[y];
			float[] v = b.data[y];
			float[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]+v[x];
			}
		}
		return c;
	}

	public static FloatFrame plus(FloatFrame a, float v) {
		FloatFrame c = FloatFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			float[] u = a.data[y];
			float[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x] + v;
			}
		}
		return c;	
	}

	public static FloatFrame div(FloatFrame a, FloatFrame b) {
		FloatFrame c = FloatFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			float[] u = a.data[y];
			float[] v = b.data[y];
			float[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = u[x]/v[x];
			}
		}
		return c;
	}

	public static FloatFrame normalised_difference(FloatFrame a, FloatFrame b) {
		FloatFrame c = FloatFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			float[] u = a.data[y];
			float[] v = b.data[y];
			float[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				w[x] = (u[x]-v[x])/(u[x]+v[x]);
			}
		}
		return c;
	}

	public FloatFrame denoise() {
		FloatFrame r = FloatFrame.ofExtent(this);
		for (int y = 1; y < height-1; y++) {
			float[] row0 = data[y];
			float[] row1 = data[y];
			float[] row2 = data[y];
			float[] rrow = r.data[y];
			for (int x = 1; x < width-1; x++) {
				float mean = (row0[x-1]+row0[x]+row0[x+1]+row1[x-1]/*+row1[x]*/+row1[x+1]+row2[x-1]+row2[x]+row2[x+1])/9f;
				float dmean = mean-rrow[x];
				rrow[x] += dmean/2d; 
			}
		}
		return r;
	}

	public FloatFrame addThis(FloatFrame frame) {
		for (int y = 0; y < height; y++) {
			float[] t = data[y];
			float[] s = frame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] += s[x];
			}
		}
		return this;
	}

	public FloatFrame substractThis(FloatFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			float[] t = data[y];
			float[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] -= s[x];
			}
		}
		return this;
	}

	public FloatFrame divThis(FloatFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			float[] t = data[y];
			float[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] /= s[x];
			}
		}
		return this;
	}

	public FloatFrame mulThis(FloatFrame baseFrame) {
		for (int y = 0; y < height; y++) {
			float[] t = data[y];
			float[] s = baseFrame.data[y];
			for (int x = 0; x < width; x++) {
				t[x] *= s[x];
			}
		}
		return this;
	}

	public ShortFrame toFrame(float scaleFactor) {		
		int w = this.width;
		int h = this.height;		
		short[][] data = new short[h][w];
		ShortFrame r = new ShortFrame(data, this.local_min_x, this.local_min_y, this.local_max_x, this.local_max_y);

		float[][] src = this.data;
		for(int y=0;y<h;y++) {
			float[] src_row = src[y];
			short[] r_row = data[y];
			for(int x=0;x<w;x++) {
				r_row[x] = (short) (src_row[x] * scaleFactor);
			}
		}		
		return r;
	}

	public void fill(float constant) {
		for(float[] row:data) {
			Arrays.fill(row, constant);
		}

	}

	/**
	 * get range of values without zero-values.
	 * @return array with a[0] == min and a[1] == max
	 */
	public float[] getMinMax() {
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for(int y=0;y<height;y++) {
			float[] row = data[y];
			for(int x=0;x<width;x++) {
				float v = row[x];
				if(v<min) {
					min = v;
				}
				if(max<v) {
					max = v;
				}
			}
		}
		return new float[]{min,max};
	}

	public static FloatFrame euclidean_distance(FloatFrame[] frames) {
		FloatFrame result = FloatFrame.ofExtent(frames[0]);
		float[][] resultData = result.data;
		int dims = frames.length;
		int width = result.width;
		int height = result.height;
		for(int d=0; d<dims; d++) {
			float[][] data = frames[d].data;
			for (int y = 0; y < height; y++) {
				float[] sourceRow = data[y];
				float[] resultRow = resultData[y];
				for (int x = 0; x < width; x++) {
					resultRow[x] += sourceRow[x] * sourceRow[x];
				}
			}
		}
		for (int y = 0; y < height; y++) {
			float[] resultRow = resultData[y];
			for (int x = 0; x < width; x++) {
				resultRow[x] = (float) Math.sqrt(resultRow[x]);
			}
		}
		return result;
	}

	public static FloatFrame mean(FloatFrame frameR, FloatFrame frameB) {
		FloatFrame frameG = FloatFrame.ofExtent(frameR);
		float[][] dataR = frameR.data;
		float[][] dataG = frameG.data;
		float[][] dataB = frameB.data;
		int dWidth = frameG.width;
		int dHeight = frameG.height;
		for (int y = 0; y < dHeight; y++) {
			float[] rowR = dataR[y];
			float[] rowB = dataB[y];
			float[] rowG = dataG[y];
			for (int x = 0; x < dWidth; x++) {
				rowG[x] = (rowR[x] + rowB[x]) / 2f;
			}
		}
		return frameG;
	}

	public BooleanFrame toMask() {
		boolean[][] mask = new boolean[height][width];
		for(int y = 0;y < height; y++) {
			float[] Row = data[y];
			boolean[] maskRow = mask[y];
			for(int x = 0;x < width; x++) {
				maskRow[x] = Float.isFinite(Row[x]);
			}
		}
		return new BooleanFrame(mask, local_min_x, local_min_y, local_max_x, local_max_y);		
	}

	public void drawBorder(float v) {
		{
			float[] y0 = data[0];
			float[] y1 = data[height - 1];
			for(int i = 0; i < width; i++) {
				y0[i] = v;
				y1[i] = v;
			}
		}
		{
			int x1 = width - 1;
			for(int i = 0; i < height; i++) {
				float[] y = data[i];
				y[0] = v;
				y[x1] = v;
			}
		}

	}	
}
