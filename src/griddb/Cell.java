package griddb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

import com.github.luben.zstd.Zstd;

public class Cell {
	//private static final Logger log = LogManager.getLogger();

	public final int x;
	public final int y;
	public final int z;
	private byte[] data;

	public final int column_count;
	private byte[] column_ids;
	private int[] column_offsets;
	private int[] column_sizes;
	
	public Cell(int x, int y, int z, byte[] data_compressed) throws IOException {
		long size = Zstd.decompressedSize(data_compressed);
		//log.info(data_compressed.length + " -> " + size);
		this.data = Zstd.decompress(data_compressed, (int) size);
		this.x = x;
		this.y = y;
		this.z = z;
		ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		this.column_count = byteBuffer.get();
		this.column_ids = new byte[column_count];
		byteBuffer.get(column_ids);
		this.column_offsets = new int[column_count];
		this.column_sizes = new int[column_count];
		int offset = 1 + column_count * (1 + 4);
		for (int i = 0; i < column_count; i++) {
			column_offsets[i] = offset;
			int column_size = byteBuffer.getInt();
			column_sizes[i] = column_size;
			offset += column_size;
		}
	}
	
	public static byte[] recompressData(byte[] data_compressed, int level) {
		long size = Zstd.decompressedSize(data_compressed);
		byte[] result = Zstd.decompress(data_compressed, (int) size);
		byte[] result_compressed = Zstd.compress(result, level);
		return result_compressed;
	}
	
	public static byte[] createData(Attribute[] attributes, byte[][] columns, int column_count) {
		int sum = 0;
		for (int i = 0; i < column_count; i++) {
			sum += columns[i].length;
		}
		byte[] result = new byte[1 + column_count + 4*column_count + sum];
		ByteBuffer byteBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte)column_count);
		byte[] ci = new byte[column_count];
		for (int i = 0; i < column_count; i++) {
			ci[i] = attributes[i].id;
		}
		byteBuffer.put(ci);
		for (int i = 0; i < column_count; i++) {
			int size = columns[i].length;
			byteBuffer.putInt(size);
		}
		for (int i = 0; i < column_count; i++) {
			byteBuffer.put(columns[i]);
		}
		byte[] result_compressed = Zstd.compress(result, 1); // default
		//byte[] result_compressed = Zstd.compress(result, 6); // best size / speed ratio
		//byte[] result_compressed = Zstd.compress(result, 22); // maximum
		//log.info("compressed "+result.length+" -> "+result_compressed.length);
		return result_compressed;
	}

	private int getIndex(int id) {
		for (int i = 0; i < column_count; i++) {
			if(column_ids[i] == id) {
				return i;
			}
		}
		return -1;
	}
	
	public long[] getLong(Attribute attr) {
		if(attr == null) {
			return null;
		}
		int index = getIndex(attr.id);
		if(index < 0) {
			return null;
		}
		return Encoding.getLong(attr.encoding, data, column_offsets[index], column_sizes[index]);
	}

	public int[] getInt(Attribute attr) {
		if(attr == null) {
			return null;
		}
		int index = getIndex(attr.id);
		if(index < 0) {
			return null;
		}
		return Encoding.getInt(attr.encoding, data, column_offsets[index], column_sizes[index]);
	}
	
	public char[] getChar(Attribute attr) {
		if(attr == null) {
			return null;
		}
		int index = getIndex(attr.id);
		if(index < 0) {
			return null;
		}
		return Encoding.getChar(attr.encoding, data, column_offsets[index], column_sizes[index]);
	}
	
	public byte[] getByte(Attribute attr) {
		if(attr == null) {
			return null;
		}
		int index = getIndex(attr.id);
		if(index < 0) {
			return null;
		}
		return Encoding.getByte(attr.encoding, data, column_offsets[index], column_sizes[index]);
	}
	
	public BitSet getBitSet(Attribute attr) {
		if(attr == null) {
			return null;
		}
		int index = getIndex(attr.id);
		if(index < 0) {
			return null;
		}
		return Encoding.getBitSet(attr.encoding, data, column_offsets[index], column_sizes[index]);
	}

	@Override
	public String toString() {
		return "cell["+x+" "+y+"]";
	}
}