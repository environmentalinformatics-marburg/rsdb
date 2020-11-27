package util.frame;

import java.util.Map;
import java.util.TreeMap;

import util.Range2d;

public class ByteFrame {
	public final byte[][] data;
	public final int width;
	public final int height;

	public final int local_min_x;
	public final int local_min_y;
	public final int local_max_x;
	public final int local_max_y;

	/**
	 * map of meta entries.
	 * currently not serialized.
	 */
	public final Map<String, Object> meta = new TreeMap<String, Object>();

	public ByteFrame(byte[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
		//setToNa();
	}
	
	public static ByteFrame of(byte[][] data, Range2d range2d) {
		return new ByteFrame(data, range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);
		
	}

	/**
	 * get range of values without zero-values.
	 * @return array with a[0] == min and a[1] == max
	 */
	public int[] getMinMax0() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(int y=0;y<height;y++) {
			byte[] row = data[y];
			for(int x=0;x<width;x++) {
				byte v = row[x];
				if(v!=0) {
					if(v<min) {
						min = v;
					}
					if(max<v) {
						max = v;
					}
				}
			}
		}
		return new int[]{min,max};
	}
	
	public static ByteFrame ofExtent(ByteFrame extent) {
		return new ByteFrame(new byte[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static ByteFrame ofExtent(FloatFrame extent) {
		return new ByteFrame(new byte[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static ByteFrame ofExtent(ShortFrame extent) {
		return new ByteFrame(new byte[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	public static ByteFrame avg(ByteFrame a, ByteFrame b) {
		byte[][] data = new byte[a.height][a.width];
		for(int y=0;y<a.height;y++) {
			byte[] aRow = a.data[y];
			byte[] bRow = b.data[y];
			byte[] row = data[y];
			for(int x=0;x<a.width;x++) {
				row[x] = (byte) ((aRow[x]+bRow[x])/2);
			}
		}
		return new ByteFrame(data, a.local_min_x, a.local_min_y, a.local_max_x, a.local_max_y);
	}
	
	public static ByteFrame scale(ByteFrame frame, int scale) {
		byte[][] data = new byte[frame.height*scale][frame.width*scale];
		ByteFrame r = new ByteFrame(data, frame.local_min_x, frame.local_min_y, frame.local_max_x, frame.local_max_y);
		
		byte[][] src = frame.data;
		int ry=0;
		for(int y=0;y<frame.height;y++) {
			int rx=0;
			byte[] src_row = src[y];
			byte[] r_row = data[ry++];
			for(int x=0;x<frame.width;x++) {
				byte v = src_row[x];
				for(int i=0;i<scale;i++) {
					r_row[rx++] = v;
				}
			}
			for(int i=1;i<scale;i++) {
				System.arraycopy(r_row, 0, data[ry++], 0, r_row.length);
			}
		}		
		return r;		
	}	
	
	public static ByteFrame mean(ByteFrame frameR, ByteFrame frameB) {
		ByteFrame frameG = ByteFrame.ofExtent(frameR);
		byte[][] dataR = frameR.data;
		byte[][] dataG = frameG.data;
		byte[][] dataB = frameB.data;
		int dWidth = frameG.width;
		int dHeight = frameG.height;
		for (int y = 0; y < dHeight; y++) {
			byte[] rowR = dataR[y];
			byte[] rowB = dataB[y];
			byte[] rowG = dataG[y];
			for (int x = 0; x < dWidth; x++) {
				rowG[x] = (byte) ((rowR[x] + rowB[x]) >> 1);
			}
		}
		return frameG;
	}

	public static ByteFrame ofFloats(FloatFrame source, byte na_target) {
		ByteFrame target = ofExtent(source);
		floatToByte(source.data, target.data, source.width, source.height, na_target);
		return target;
	}
	
	public static void floatToByte(float[][] src, byte[][] dst, int width, int height, byte na_target) {
		for (int y = 0; y < height; y++) {
			float[] s = src[y];
			byte[] t = dst[y];
			for (int x = 0; x < width; x++) {
				float v = s[x];				
				t[x] = Float.isFinite(v) ? (byte) v : na_target;
			}
		}
	}
	
	public static ByteFrame ofShorts(ShortFrame source, short na_src, byte na_target) {
		ByteFrame target = ofExtent(source);
		shortToByte(source.data, target.data, source.width, source.height, na_src, na_target);
		return target;
	}
	
	public static void shortToByte(short[][] src, byte[][] dst, int width, int height, short na_src, byte na_target) {
		for (int y = 0; y < height; y++) {
			short[] s = src[y];
			byte[] t = dst[y];
			for (int x = 0; x < width; x++) {
				short v = s[x];				
				t[x] = v == na_src ? na_target: (byte) v;
			}
		}
	}
	
	public BooleanFrame toMask(byte na) {
		boolean[][] mask = new boolean[height][width];
		for(int y = 0;y < height; y++) {
			byte[] Row = data[y];
			boolean[] maskRow = mask[y];
			for(int x = 0;x < width; x++) {
				maskRow[x] = Row[x] != na;
			}
		}
		return new BooleanFrame(mask, local_min_x, local_min_y, local_max_x, local_max_y);		
	}	
}