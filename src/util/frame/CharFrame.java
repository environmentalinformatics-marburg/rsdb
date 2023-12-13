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

public class CharFrame {

	public final char[][] data;
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

	public CharFrame(char[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
		//setToNa();
	}
	
	public static CharFrame of(char[][] data, Range2d range2d) {
		return new CharFrame(data, range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);
		
	}

	/**
	 * get range of values without zero-values.
	 * @return array with a[0] == min and a[1] == max
	 */
	public int[] getMinMax0() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(int y=0;y<height;y++) {
			char[] row = data[y];
			for(int x=0;x<width;x++) {
				char v = row[x];
				if(v != 0) {
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
	
	public static CharFrame ofExtent(CharFrame extent) {
		return new CharFrame(new char[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static CharFrame ofExtent(ShortFrame extent) {
		return new CharFrame(new char[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static CharFrame ofExtent(ByteFrame extent) {
		return new CharFrame(new char[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static CharFrame ofExtent(FloatFrame extent) {
		return new CharFrame(new char[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	public static CharFrame avg(CharFrame a, CharFrame b) {
		char[][] data = new char[a.height][a.width];
		for(int y=0;y<a.height;y++) {
			char[] aRow = a.data[y];
			char[] bRow = b.data[y];
			char[] row = data[y];
			for(int x=0;x<a.width;x++) {
				row[x] = (char) ((aRow[x]+bRow[x])/2);
			}
		}
		return new CharFrame(data, a.local_min_x, a.local_min_y, a.local_max_x, a.local_max_y);
	}
	
	public static CharFrame normalised_difference_x10000(CharFrame a, CharFrame b) {
		CharFrame c = CharFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			char[] u = a.data[y];
			char[] v = b.data[y];
			char[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				double va = u[x];
				double vb = v[x];
				w[x] = (char) (((va-vb)/(va+vb)) * 10000D);
			}
		}
		return c;
	}


	/**
	 * Serializer without meta
	 * @author woellauer
	 *
	 */
	private static final class FrameSerializer extends Serializer<CharFrame> {

		private final static byte current_version = 1;

		@Override
		public void serialize(DataOutput out, CharFrame f) throws IOException {
			out.writeByte(current_version);
			out.writeInt(f.width);
			out.writeInt(f.height);			
			out.writeInt(f.local_min_x);
			out.writeInt(f.local_min_y);
			out.writeInt(f.local_max_x);
			out.writeInt(f.local_max_y);			
			
			final int length = f.height*f.width;
			char[][] data = f.data;
			int[] raw = new int[length];
			int destPos = 0;
			for(int i=0;i<length;i++) {
				char[] src = data[i];				
				for(int c=0;c<length;c++) {
					raw[destPos++] = src[c];
				}
			}
			IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();
			Serialisation.encodeDeltaZigZag(raw);
			Serialisation.writeCompressIntArray(out, ic.compress(raw));
		}

		@Override
		public CharFrame deserialize(DataInput in, int available) throws IOException {
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

			char[][] data = new char[height][width];
			int srcPos = 0;
			for(int i=0;i<height;i++) {
				char[] dst = data[i];				
				for(int c=0;c<width;c++) {
					dst[c] = (char) raw[srcPos++];
				}
			}

			return new CharFrame(data, local_min_x, local_min_y, local_max_x, local_max_y);
		}

	}
	
	private static final Serializer<CharFrame> SERIALIZER = new FrameSerializer();

	public static Serializer<CharFrame> getSerializer() {
		return SERIALIZER;
	}
	
	public static CharFrame scale(CharFrame frame, int scale) {
		char[][] data = new char[frame.height*scale][frame.width*scale];
		CharFrame r = new CharFrame(data, frame.local_min_x, frame.local_min_y, frame.local_max_x, frame.local_max_y);
		
		char[][] src = frame.data;
		int ry=0;
		for(int y=0;y<frame.height;y++) {
			int rx=0;
			char[] src_row = src[y];
			char[] r_row = data[ry++];
			for(int x=0;x<frame.width;x++) {
				char v = src_row[x];
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
	
	
	public static CharFrame mean(CharFrame frameR, CharFrame frameB) {
		CharFrame frameG = CharFrame.ofExtent(frameR);
		char[][] dataR = frameR.data;
		char[][] dataG = frameG.data;
		char[][] dataB = frameB.data;
		int dWidth = frameG.width;
		int dHeight = frameG.height;
		for (int y = 0; y < dHeight; y++) {
			char[] rowR = dataR[y];
			char[] rowB = dataB[y];
			char[] rowG = dataG[y];
			for (int x = 0; x < dWidth; x++) {
				rowG[x] = (char) ((rowR[x] + rowB[x]) >> 1);
			}
		}
		return frameG;
	}

	public static CharFrame ofFloats(FloatFrame source, char na_target) {
		CharFrame target = ofExtent(source);
		floatToChar(source.data, target.data, source.width, source.height, na_target);
		return target;
	}
	
	public static void floatToChar(float[][] src, char[][] dst, int width, int height, char na_target) {
		for (int y = 0; y < height; y++) {
			float[] s = src[y];
			char[] t = dst[y];
			for (int x = 0; x < width; x++) {
				float v = s[x];				
				t[x] = Float.isFinite(v) ? (char) v : na_target;
			}
		}
	}
	
	public static CharFrame ofShorts(ShortFrame source, short na_src, char na_dst) {
		CharFrame target = ofExtent(source);
		shortToChar(source.data, target.data, source.width, source.height, na_src, na_dst);
		return target;
	}
	
	public static void shortToChar(short[][] src, char[][] dst, int width, int height, short na_src, char na_dst) {
		for (int y = 0; y < height; y++) {
			short[] s = src[y];
			char[] t = dst[y];
			for (int x = 0; x < width; x++) {
				short v = s[x];				
				t[x] = v == na_src ? na_dst : (char) v;
			}
		}
	}
	
	
	public static CharFrame ofBytes(ByteFrame source, byte na_src, char na_target) {
		CharFrame target = ofExtent(source);
		byteToChar(source.data, target.data, source.width, source.height, na_src, na_target);
		return target;
	}
	
	public static void byteToChar(byte[][] src, char[][] dst, int width, int height, byte na_src, char na_target) {
		for (int y = 0; y < height; y++) {
			byte[] s = src[y];
			char[] t = dst[y];
			for (int x = 0; x < width; x++) {
				byte v = s[x];				
				t[x] = v == na_src ? na_target: (char) v;
			}
		}
	}
	
	public BooleanFrame toMask(char na) {
		boolean[][] mask = new boolean[height][width];
		for(int y = 0;y < height; y++) {
			char[] Row = data[y];
			boolean[] maskRow = mask[y];
			for(int x = 0;x < width; x++) {
				maskRow[x] = Row[x] != na;
			}
		}
		return new BooleanFrame(mask, local_min_x, local_min_y, local_max_x, local_max_y);		
	}
	
	public static void rawMul(char[][] data, double factor) {
		for(char[] row:data) {
			int len = row.length;
			for(int i=0; i<len; i++) {
				row[i] *= factor;
			}
		}
	}

	public static void rawAdd(char[][] data, double add) {
		for(char[] row:data) {
			int len = row.length;
			for(int i=0; i<len; i++) {
				row[i] += add;
			}
		}
	}	
}
