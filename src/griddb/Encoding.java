package griddb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

import me.lemire.integercompression.FastPFOR128;
import me.lemire.integercompression.IntCompressor;
import me.lemire.integercompression.SkippableComposition;
import me.lemire.integercompression.VariableByte;
import util.Serialisation;

public class Encoding {
	//private static final Logger log = LogManager.getLogger();

	private static ThreadLocal<IntCompressor> threadLocal_ic = new ThreadLocal<IntCompressor>() {
		@Override
		protected IntCompressor initialValue() {
			SkippableComposition codec = new SkippableComposition(new FastPFOR128(), new VariableByte());
			IntCompressor ic = new IntCompressor(codec);
			return ic;
		}		
	};	

	public static final int ENCODING_INT32_DELTA_ZIGZAG_PFOR = 1;
	public static final int ENCODING_UINT16_PFOR = 2;
	public static final int ENCODING_UINT16 = 3;
	public static final int ENCODING_UINT8 = 4;
	public static final int ENCODING_INT8 = 5;
	public static final int ENCODING_INT8_DELTA = 6;
	public static final int ENCODING_INT64 = 7;
	public static final int ENCODING_INT64_DELTA = 8;
	public static final int ENCODING_INT64_DELTA_ZIGZAG_SPLIT = 9;
	public static final int ENCODING_INT32 = 10;
	public static final int ENCODING_INT32_PFOR = 11;
	public static final int ENCODING_INT64_DELTA_ZIGZAG_SPLIT_SPLIT = 12;
	public static final int ENCODING_BITSET = 13;
	public static final int ENCODING_UINT16_SPLIT = 14;

	public static int[] decInt32_delta_zigzag_pfor(byte[] compressed, int off, int len) {
		int[] ints = decInt32_pfor_internal(compressed, off, len);
		Serialisation.decodeDeltaZigZag(ints);
		return ints;
	}

	public static int[] decInt32(byte[] compressed, int off, int len) {
		int[] data = Serialisation.byteToIntArray(compressed, off, len);
		return data;
	}

	public static int[] decInt32_pfor(byte[] compressed, int off, int len) {
		return decInt32_pfor_internal(compressed, off, len);
	}

	public static int[] decInt32_pfor_internal(byte[] compressed, int off, int len) {
		int[] data = Serialisation.byteToIntArray(compressed, off, len);
		IntCompressor ic = threadLocal_ic.get();
		int[] ints = ic.uncompress(data);
		return ints;
	}
	
	public static int[] decInt32_pfor_internal(int[] compressed) {
		IntCompressor ic = threadLocal_ic.get();
		int[] ints = ic.uncompress(compressed);
		return ints;
	}

	public static char[] decUint16_pfor(byte[] compressed, int off, int len) {
		int[] ints = decInt32_pfor_internal(compressed, off, len);
		char[] result = Serialisation.copyInt32ToUint16(ints);
		return result;
	}

	public static char[] decUint16(byte[] compressed, int off, int len) {
		char[] result = Serialisation.byteToCharArray(compressed, off, len);
		return result;		
	}

	public static char[] decUint16_split(byte[] compressed, int off, int len) {		
		int size = len >> 1;		
		char[] result = new char[size];
		int pos_lower = off;
		int pos_upper = off + size;
		for(int i = 0; i < size; i++) {
			result[i] = (char) ((compressed[i + pos_lower] & 0xFF) | ((compressed[i + pos_upper] & 0xFF)<<8));
		}
		return result;
	}

	public static long[] decInt64(byte[] compressed, int off, int len) {
		long[] result = Serialisation.byteToLongArray(compressed, off, len);
		return result;
	}

	public static long[] decInt64_delta(byte[] compressed, int off, int len) {
		long[] result = Serialisation.byteToLongArray(compressed, off, len);
		Serialisation.decodeDelta(result);
		return result;
	}

	public static long[] decInt64_delta_zigzag_split(byte[] compressed, int off, int len) {
		int lenHalf = len >> 1;
			int[] lower = Serialisation.byteToIntArray(compressed, off, lenHalf);
			int[] upper = Serialisation.byteToIntArray(compressed, off + lenHalf, lenHalf);
			int long_len = lenHalf >> 2;
		long[] result = new long[long_len];
		for (int i = 0; i < long_len; i++) {			
			result[i] = (long)lower[i] | ((long)upper[i]<<32);
		}
		Serialisation.decodeDeltaZigZag(result);
		return result;
	}

	public static long[] decInt64_delta_zigzag_split_split(byte[] compressed, int off, int len) {
		long[] result = decInt64_split_split(compressed, off, len);
		Serialisation.decodeDeltaZigZag(result);
		return result;
	}

	public static long[] decInt64_split_split(byte[] compressed, int off, int len) {
		int lenQuarter = len >> 2;
			char[] lower_lower = Serialisation.byteToCharArray(compressed, off, lenQuarter);
			char[] lower_upper = Serialisation.byteToCharArray(compressed, off + lenQuarter, lenQuarter);
			int lenHalf = len >> 1;
		int[] upper = Serialisation.byteToIntArray(compressed, off + lenHalf, lenHalf);
		int long_len = lenHalf >> 2;
		long[] result = new long[long_len];
		for (int i = 0; i < long_len; i++) {			
			result[i] = (long)upper[i] << 32 | ((lower_upper[i] & 0xFFFFL) << 16) | lower_lower[i] & 0xFFFFL;
		}
		return result;
	}

	public static byte[] encInt32(int[] data, int rows) {
		return Serialisation.intToByteArray(data, rows);
	}

	public static byte[] encInt32_pfor(int[] data, int rows) {
		data = rows < data.length ? Arrays.copyOf(data, rows) : data;
		return Serialisation.intToByteArray(encInt32_pfor_internal(data));
	}
	
	public static byte[] encInt32_delta_zigzag_pfor(int[] data, int rows) {
		return Serialisation.intToByteArray(encInt32_pfor_internal(Serialisation.encodeDeltaZigZagCopy(data, rows)));
	}

	public static int[] encInt32_pfor_internal(int[] data) {
		IntCompressor ic = threadLocal_ic.get();
		int[] moved = ic.compress(data);
		return moved;
	}

	public static byte[] encUint16_pfor(char[] data, int rows) {
		return Serialisation.intToByteArray(encInt32_pfor_internal(Serialisation.copyUint16ToInt32(data, rows)));
	}

	public static byte[] encUint16(char[] data, int rows) {		
		return Serialisation.charToByteArray(data, rows);
	}

	public static byte[] encUint16_split(char[] data, int rows) {
		byte[] result = new byte[rows * 2];
		for (int i = 0; i < rows; i++) {
			char v = data[i];
			result[i] = (byte) v;
			result[i + rows] = (byte) (v>>8);
		}
		return result;
	}

	public static int[] getInt(int encoding, byte[] data, int off, int len) {
		switch(encoding) {
		case Encoding.ENCODING_INT32:
			return Encoding.decInt32(data, off, len);
		case Encoding.ENCODING_INT32_PFOR:
			return Encoding.decInt32_pfor(data, off, len);
		case Encoding.ENCODING_INT32_DELTA_ZIGZAG_PFOR:
			return Encoding.decInt32_delta_zigzag_pfor(data, off, len);
		default:
			throw new RuntimeException();
		}
	}

	public static long[] getLong(int encoding, byte[] data, int off, int len) {
		switch(encoding) {
		case Encoding.ENCODING_INT64:
			return Encoding.decInt64(data, off, len);
		case Encoding.ENCODING_INT64_DELTA:
			return Encoding.decInt64_delta(data, off, len);
		case Encoding.ENCODING_INT64_DELTA_ZIGZAG_SPLIT:
			return Encoding.decInt64_delta_zigzag_split(data, off, len);
		case Encoding.ENCODING_INT64_DELTA_ZIGZAG_SPLIT_SPLIT:
			return Encoding.decInt64_delta_zigzag_split_split(data, off, len);
		default:
			throw new RuntimeException();
		}
	}

	public static BitSet getBitSet(int encoding, byte[] data, int off, int len) {
		switch(encoding) {
		case Encoding.ENCODING_BITSET:			
			return BitSet.valueOf(ByteBuffer.wrap(data, off, len));
		default:
			throw new RuntimeException();
		}
	}

	public static char[] getChar(int encoding, byte[] data, int off, int len) {
		switch(encoding) {
		case Encoding.ENCODING_UINT16_PFOR:
			return Encoding.decUint16_pfor(data, off, len);
		case Encoding.ENCODING_UINT16:
			return Encoding.decUint16(data, off, len);
		case Encoding.ENCODING_UINT16_SPLIT:
			return Encoding.decUint16_split(data, off, len);
		default:
			throw new RuntimeException();
		}
	}

	public static byte[] getByte(int encoding, byte[] data, int off, int len) {
		switch(encoding) {
		case Encoding.ENCODING_UINT8:
		case Encoding.ENCODING_INT8: {
			byte[] result = new byte[len];
			System.arraycopy(data, off, result, 0, len);
			return result;
		}
		case Encoding.ENCODING_INT8_DELTA: {
			return Serialisation.decodeDeltaCopy(data, off, len);
		}
		default:
			throw new RuntimeException();
		}
	}

	public static byte[] createIntData(int encoding, int[] data, int rows) throws IOException {
		switch(encoding) {
		case ENCODING_INT32:
			return Encoding.encInt32(data, rows);
		case ENCODING_INT32_PFOR:
			return Encoding.encInt32_pfor(data, rows);
		case Encoding.ENCODING_INT32_DELTA_ZIGZAG_PFOR:
			return Encoding.encInt32_delta_zigzag_pfor(data, rows);
		default:
			throw new RuntimeException();
		}
	}

	public static byte[] createCharData(int encoding, char[] data, int rows) {
		switch(encoding) {
		case Encoding.ENCODING_UINT16_PFOR:
			return Encoding.encUint16_pfor(data, rows);
		case Encoding.ENCODING_UINT16:
			return Encoding.encUint16(data, rows);
		case Encoding.ENCODING_UINT16_SPLIT:
			return Encoding.encUint16_split(data, rows);
		default:
			throw new RuntimeException();
		}
	}

	public static byte[] createBitSetData(int encoding, BitSet data, int rows) throws IOException {
		switch(encoding) {
		case Encoding.ENCODING_BITSET: // row number ignored
			return data.toByteArray();
		default:
			throw new RuntimeException();
		}
	}

	public static byte[] createByteData(int encoding, byte[] data, int rows) throws IOException {
		switch(encoding) {
		case Encoding.ENCODING_UINT8:
		case Encoding.ENCODING_INT8:
			return Arrays.copyOf(data, rows);
		case Encoding.ENCODING_INT8_DELTA:
			return Serialisation.encodeDeltaCopy(data, rows);
		default:
			throw new RuntimeException();
		}
	}

	public static byte[] encInt64(long[] data, int rows) {		
		return Serialisation.int64ToByteArray(data, rows);
	}

	public static byte[] encInt64_delta(long[] data, int rows) {
		long[] delta = Serialisation.encodeDeltaCopy(data, rows);
		return Serialisation.int64ToByteArray(delta, rows);
	}

	public static byte[] encInt64_delta_zigzag_split(long[] data, int rows) {
		long[] delta = Serialisation.encodeDeltaZigZagCopy(data, rows);
		int[] upper = new int[rows];
		int[] lower = new int[rows];
		for (int i = 0; i < rows; i++) {
			long v = delta[i];
			upper[i] = (int) (v>>32);
			lower[i] = (int) v;
		}
		byte[] buf = new byte[rows*8];
		Serialisation.intToByteArray(lower, lower.length, buf, 0);
		Serialisation.intToByteArray(upper, upper.length, buf, lower.length*4);
		return buf; 
	}

	public static byte[] encInt64_delta_zigzag_split_split(long[] data, int rows) {
		long[] delta = Serialisation.encodeDeltaZigZagCopy(data, rows);
		return encInt64_split_split(delta, rows); 
	}

	public static byte[] encInt64_split_split(long[] data, int rows) {
		char[] lower_lower = new char[rows];
		char[] lower_upper = new char[rows];
		int[] upper = new int[rows];
		for (int i = 0; i < rows; i++) {
			long v = data[i];
			lower_lower[i] = (char) v;
			lower_upper[i] = (char) (v>>16);
			upper[i] = (int) (v>>32);
		}
		byte[] buf = new byte[rows*8];
		Serialisation.charToByteArray(lower_lower, rows, buf, 0);
		Serialisation.charToByteArray(lower_upper, rows, buf, rows*2);
		Serialisation.intToByteArray(upper, upper.length, buf, rows*4);
		return buf; 
	}

	public static byte[] createLongData(int encoding, long[] data, int rows) throws IOException {
		switch(encoding) {
		case Encoding.ENCODING_INT64:
			return encInt64(data, rows);
		case Encoding.ENCODING_INT64_DELTA:
			return encInt64_delta(data, rows);
		case Encoding.ENCODING_INT64_DELTA_ZIGZAG_SPLIT:
			return encInt64_delta_zigzag_split(data, rows);
		case Encoding.ENCODING_INT64_DELTA_ZIGZAG_SPLIT_SPLIT:
			return encInt64_delta_zigzag_split_split(data, rows);
		default:
			throw new RuntimeException();
		}
	}
}
