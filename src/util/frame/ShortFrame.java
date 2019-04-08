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

public class ShortFrame {

	public final short[][] data;
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

	public ShortFrame(short[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
		//setToNa();
	}
	
	public static ShortFrame of(short[][] data, Range2d range2d) {
		return new ShortFrame(data, range2d.xmin, range2d.ymin, range2d.xmax, range2d.ymax);
		
	}

	/**
	 * get range of values without zero-values.
	 * @return array with a[0] == min and a[1] == max
	 */
	public int[] getMinMax0() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(int y=0;y<height;y++) {
			short[] row = data[y];
			for(int x=0;x<width;x++) {
				short v = row[x];
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
	
	public static ShortFrame ofExtent(ShortFrame extent) {
		return new ShortFrame(new short[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}
	
	public static ShortFrame ofExtent(FloatFrame extent) {
		return new ShortFrame(new short[extent.height][extent.width], extent.local_min_x, extent.local_min_y, extent.local_max_x, extent.local_max_y);
	}

	public static ShortFrame avg(ShortFrame a, ShortFrame b) {
		short[][] data = new short[a.height][a.width];
		for(int y=0;y<a.height;y++) {
			short[] aRow = a.data[y];
			short[] bRow = b.data[y];
			short[] row = data[y];
			for(int x=0;x<a.width;x++) {
				row[x] = (short) ((aRow[x]+bRow[x])/2);
			}
		}
		return new ShortFrame(data, a.local_min_x, a.local_min_y, a.local_max_x, a.local_max_y);
	}
	
	public static ShortFrame normalised_difference_x10000(ShortFrame a, ShortFrame b) {
		ShortFrame c = ShortFrame.ofExtent(a);		
		for (int y = 0; y < a.height; y++) {
			short[] u = a.data[y];
			short[] v = b.data[y];
			short[] w = c.data[y];
			for (int x = 0; x < a.width; x++) {
				double va = u[x];
				double vb = v[x];
				w[x] = (short) (((va-vb)/(va+vb)) * 10000D);
			}
		}
		return c;
	}


	/*public void setToNa() {
		for(int y=0;y<height;y++) {
			short[] row = data[y];
			for(int x=0;x<width;x++) {
				short v = row[x];
				row[x] = Short.MIN_VALUE;
			}
		}		
	}*/

	/**
	 * Serializer without meta
	 * @author woellauer
	 *
	 */
	private static final class FrameSerializer extends Serializer<ShortFrame> {

		private final static byte current_version = 1;

		@Override
		public void serialize(DataOutput out, ShortFrame f) throws IOException {
			out.writeByte(current_version);
			out.writeInt(f.width);
			out.writeInt(f.height);			
			out.writeInt(f.local_min_x);
			out.writeInt(f.local_min_y);
			out.writeInt(f.local_max_x);
			out.writeInt(f.local_max_y);			
			
			final int length = f.height*f.width;
			short[][] data = f.data;
			int[] raw = new int[length];
			int destPos = 0;
			for(int i=0;i<length;i++) {
				short[] src = data[i];				
				for(int c=0;c<length;c++) {
					raw[destPos++] = src[c];
				}
			}
			IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();
			Serialisation.encodeDeltaZigZag(raw);
			Serialisation.writeCompressIntArray(out, ic.compress(raw));
		}

		@Override
		public ShortFrame deserialize(DataInput in, int available) throws IOException {
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

			short[][] data = new short[height][width];
			int srcPos = 0;
			for(int i=0;i<height;i++) {
				short[] dst = data[i];				
				for(int c=0;c<width;c++) {
					dst[c] = (short) raw[srcPos++];
				}
			}

			return new ShortFrame(data, local_min_x, local_min_y, local_max_x, local_max_y);
		}

	}
	
	private static final Serializer<ShortFrame> SERIALIZER = new FrameSerializer();

	public static Serializer<ShortFrame> getSerializer() {
		return SERIALIZER;
	}
	
	public static ShortFrame scale(ShortFrame frame, int scale) {
		short[][] data = new short[frame.height*scale][frame.width*scale];
		ShortFrame r = new ShortFrame(data, frame.local_min_x, frame.local_min_y, frame.local_max_x, frame.local_max_y);
		
		short[][] src = frame.data;
		int ry=0;
		for(int y=0;y<frame.height;y++) {
			int rx=0;
			short[] src_row = src[y];
			short[] r_row = data[ry++];
			for(int x=0;x<frame.width;x++) {
				short v = src_row[x];
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
	
	
	public static ShortFrame mean(ShortFrame frameR, ShortFrame frameB) {
		ShortFrame frameG = ShortFrame.ofExtent(frameR);
		short[][] dataR = frameR.data;
		short[][] dataG = frameG.data;
		short[][] dataB = frameB.data;
		int dWidth = frameG.width;
		int dHeight = frameG.height;
		for (int y = 0; y < dHeight; y++) {
			short[] rowR = dataR[y];
			short[] rowB = dataB[y];
			short[] rowG = dataG[y];
			for (int x = 0; x < dWidth; x++) {
				rowG[x] = (short) ((rowR[x] + rowB[x]) >> 1);
			}
		}
		return frameG;
	}

	public static ShortFrame ofFloats(FloatFrame source, short na_target) {
		ShortFrame target = ofExtent(source);
		for (int y = 0; y < source.height; y++) {
			float[] s = source.data[y];
			short[] t = target.data[y];
			for (int x = 0; x < source.width; x++) {
				float v = s[x];				
				t[x] = Float.isFinite(v) ? (short) v : na_target;
			}
		}
		return target;
	}
	
	public BooleanFrame toMask(short na) {
		boolean[][] mask = new boolean[height][width];
		for(int y = 0;y < height; y++) {
			short[] Row = data[y];
			boolean[] maskRow = mask[y];
			for(int x = 0;x < width; x++) {
				maskRow[x] = Row[x] != na;
			}
		}
		return new BooleanFrame(mask, local_min_x, local_min_y, local_max_x, local_max_y);		
	}
	
	
}
