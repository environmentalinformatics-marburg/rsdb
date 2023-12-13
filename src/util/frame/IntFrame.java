package util.frame;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.mapdb.Serializer;

import me.lemire.integercompression.IntCompressor;
import util.Range2d;
import util.Serialisation;

public class IntFrame {

	public final int[][] data;
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

	public IntFrame(int[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
		//setToNa();
	}
	
	public static IntFrame of(int[][] data, Range2d range2d) {
		return new IntFrame(data, range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);
		
	}

	/**
	 * get range of values without zero-values.
	 * @return array with a[0] == min and a[1] == max
	 */
	public int[] getMinMax0() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(int y=0;y<height;y++) {
			int[] row = data[y];
			for(int x=0;x<width;x++) {
				int v = row[x];
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
	
	public static IntFrame ofExtent(IntFrame extent) {
		return new IntFrame(new int[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	public static IntFrame ofExtent(ShortFrame extent) {
		return new IntFrame(new int[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static IntFrame ofExtent(ByteFrame extent) {
		return new IntFrame(new int[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static IntFrame ofExtent(FloatFrame extent) {
		return new IntFrame(new int[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	public static IntFrame avg(IntFrame a, IntFrame b) {
		int[][] data = new int[a.height][a.width];
		for(int y=0;y<a.height;y++) {
			int[] aRow = a.data[y];
			int[] bRow = b.data[y];
			int[] row = data[y];
			for(int x=0;x<a.width;x++) {
				row[x] = (int) ((((long)aRow[x]) + bRow[x])/2);
			}
		}
		return new IntFrame(data, a.local_min_x, a.local_min_y, a.local_max_x, a.local_max_y);
	}
	
	public static IntFrame normalised_difference_x10000(IntFrame a, IntFrame b) {
		IntFrame c = IntFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			int[] u = a.data[y];
			int[] v = b.data[y];
			int[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				double va = u[x];
				double vb = v[x];
				w[x] = (short) (((va-vb)/(va+vb)) * 10000D);
			}
		}
		return c;
	}


	/**
	 * Serializer without meta
	 * @author woellauer
	 *
	 */
	private static final class FrameSerializer extends Serializer<IntFrame> {

		private final static byte current_version = 1;

		@Override
		public void serialize(DataOutput out, IntFrame f) throws IOException {
			out.writeByte(current_version);
			out.writeInt(f.width);
			out.writeInt(f.height);			
			out.writeInt(f.local_min_x);
			out.writeInt(f.local_min_y);
			out.writeInt(f.local_max_x);
			out.writeInt(f.local_max_y);			
			
			final int length = f.height*f.width;
			int[][] data = f.data;
			int[] raw = new int[length];
			int destPos = 0;
			for(int i=0;i<length;i++) {
				int[] src = data[i];				
				for(int c=0;c<length;c++) {
					raw[destPos++] = src[c];
				}
			}
			IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();
			Serialisation.encodeDeltaZigZag(raw);
			Serialisation.writeCompressIntArray(out, ic.compress(raw));
		}

		@Override
		public IntFrame deserialize(DataInput in, int available) throws IOException {
			byte version = in.readByte();
			if(version!=1) {
				throw new RuntimeException("unknown version "+version);
			}
			int width = in.readInt();
			int height = in.readInt();
			int local_min_x = in.readInt();
			int local_min_y = in.readInt();
			int local_max_x = in.readInt();
			int local_max_y = in.readInt();			
			
			IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();
			int[] raw = ic.uncompress(Serialisation.readUncompressIntArray(in));
			Serialisation.decodeDeltaZigZag(raw);

			int[][] data = new int[height][width];
			int srcPos = 0;
			for(int i=0;i<height;i++) {
				int[] dst = data[i];				
				for(int c=0;c<width;c++) {
					dst[c] = raw[srcPos++];
				}
			}

			return new IntFrame(data, local_min_x, local_min_y, local_max_x, local_max_y);
		}

	}
	
	private static final Serializer<IntFrame> SERIALIZER = new FrameSerializer();

	public static Serializer<IntFrame> getSerializer() {
		return SERIALIZER;
	}
	
	public static IntFrame scale(IntFrame frame, int scale) {
		int[][] data = new int[frame.height*scale][frame.width*scale];
		IntFrame r = new IntFrame(data, frame.local_min_x, frame.local_min_y, frame.local_max_x, frame.local_max_y);
		
		int[][] src = frame.data;
		int ry=0;
		for(int y=0;y<frame.height;y++) {
			int rx=0;
			int[] src_row = src[y];
			int[] r_row = data[ry++];
			for(int x=0;x<frame.width;x++) {
				int v = src_row[x];
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
	
	
	public static IntFrame mean(IntFrame frameR, IntFrame frameB) {
		IntFrame frameG = IntFrame.ofExtent(frameR);
		int[][] dataR = frameR.data;
		int[][] dataG = frameG.data;
		int[][] dataB = frameB.data;
		int dWidth = frameG.width;
		int dHeight = frameG.height;
		for (int y = 0; y < dHeight; y++) {
			int[] rowR = dataR[y];
			int[] rowB = dataB[y];
			int[] rowG = dataG[y];
			for (int x = 0; x < dWidth; x++) {
				rowG[x] = (int) (((long)(rowR[x]) + rowB[x]) >> 1);
			}
		}
		return frameG;
	}

	public static IntFrame ofFloats(FloatFrame source, int na_target) {
		IntFrame target = ofExtent(source);
		floatToInt(source.data, target.data, source.width, source.height, na_target);
		return target;
	}
	
	public static void floatToInt(float[][] src, int[][] dst, int width, int height, int na_target) {
		for (int y = 0; y < height; y++) {
			float[] s = src[y];
			int[] t = dst[y];
			for (int x = 0; x < width; x++) {
				float v = s[x];				
				t[x] = Float.isFinite(v) ? (int) v : na_target;
			}
		}
	}
	
	public static IntFrame ofShorts(ShortFrame source, short na_src, int na_target) {
		IntFrame target = ofExtent(source);
		shortToInt(source.data, target.data, source.width, source.height, na_src, na_target);
		return target;
	}
	
	public static void shortToInt(short[][] src, int[][] dst, int width, int height, short na_src, int na_target) {
		for (int y = 0; y < height; y++) {
			short[] s = src[y];
			int[] t = dst[y];
			for (int x = 0; x < width; x++) {
				short v = s[x];				
				t[x] = v == na_src ? na_target: v;
			}
		}
	}	
	
	public static IntFrame ofBytes(ByteFrame source, byte na_src, int na_target) {
		IntFrame target = ofExtent(source);
		byteToInt(source.data, target.data, source.width, source.height, na_src, na_target);
		return target;
	}
	
	public static void byteToInt(byte[][] src, int[][] dst, int width, int height, byte na_src, int na_target) {
		for (int y = 0; y < height; y++) {
			byte[] s = src[y];
			int[] t = dst[y];
			for (int x = 0; x < width; x++) {
				byte v = s[x];				
				t[x] = v == na_src ? na_target: v;
			}
		}
	}
	
	public BooleanFrame toMask(int na) {
		boolean[][] mask = new boolean[height][width];
		for(int y = 0;y < height; y++) {
			int[] Row = data[y];
			boolean[] maskRow = mask[y];
			for(int x = 0; x < width; x++) {
				maskRow[x] = Row[x] != na;
			}
		}
		return new BooleanFrame(mask, local_min_x, local_min_y, local_max_x, local_max_y);		
	}
	
	public static void rawMul(int[][] data, double factor) {
		for(int[] row:data) {
			int len = row.length;
			for(int i=0; i<len; i++) {
				row[i] *= factor;
			}
		}
	}

	public static void rawAdd(int[][] data, double add) {
		for(int[] row:data) {
			int len = row.length;
			for(int i=0; i<len; i++) {
				row[i] += add;
			}
		}
	}	
}
