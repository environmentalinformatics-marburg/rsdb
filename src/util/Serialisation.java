package util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;


import org.tinylog.Logger;
import org.mapdb.DataIO;
import org.mapdb.Serializer;
import org.xerial.snappy.Snappy;

import me.lemire.integercompression.FastPFOR128;
import me.lemire.integercompression.IntCompressor;
import me.lemire.integercompression.SkippableComposition;
import me.lemire.integercompression.VariableByte;

public final class Serialisation {
	@SuppressWarnings("unused")
	

	private Serialisation(){}

	public static void writeCompressIntArray(DataOutput out, int[] array) throws IOException {
		byte[] bytes = Snappy.compress(array);
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	public static int[] readUncompressIntArray(DataInput in) throws IOException {
		int len = in.readInt();
		//Logger.info("array size "+len);
		byte[] bytes = new byte[len];
		in.readFully(bytes);
		return Snappy.uncompressIntArray(bytes);
	}

	public static long encodeZigZag(long v) {
		return (v << 1) ^ (v >> 63);
	}

	public static int encodeZigZag(int v) {
		return (v << 1) ^ (v >> 31);
	}

	public static short encodeZigZag(short v) {
		return (short) ((v << 1) ^ (v >> 31));
	}

	public static short decodeZigZag(short v) {
		int i = v & 0xFFFF;
		return (short) ((i >>> 1) ^ ((i << 31) >> 31));
	}

	public static int decodeZigZag(int v) {
		return (v >>> 1) ^ ((v << 31) >> 31);
	}

	public static long decodeZigZag(long v) {
		return (v >>> 1) ^ ((v << 63) >> 63);
	}

	//from kryo
	public static int decodeZigZagAlternative(int v) {// same performance
		return (v >>> 1) ^ -(v & 1);
	}

	/**
	 * no delta
	 * @param data
	 */
	public static void encodeZigZag(int[] data) {
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			data[i] = encodeZigZag(data[i]);
		}
	}

	public static void encodeDeltaZigZag(int[] data) {
		int prev = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			int curr = data[i];
			data[i] = encodeZigZag(curr - prev);
			prev = curr;
		}
	}

	public static int[] encodeDeltaZigZagCopy(int[] data, int len) {
		int[] result = new int[len];
		int prev = 0;
		for (int i = 0; i < len; i++) {
			int curr = data[i];
			result[i] = encodeZigZag(curr - prev);
			prev = curr;
		}
		return result;
	}

	public static byte[] encodeDeltaCopy(byte[] data, int len) {
		byte[] result = new byte[len];
		byte prev = 0;
		for (int i = 0; i < len; i++) {
			byte curr = data[i];
			result[i] = (byte) (curr - prev);
			prev = curr;
		}
		return result;
	}

	public static int[] encodeUint16DeltaZigZagCopy(char[] data, int len) {
		int[] result = new int[len];
		int prev = 0;
		for (int i = 0; i < len; i++) {
			int curr = data[i];
			result[i] = encodeZigZag(curr - prev);
			prev = curr;
		}
		return result;
	}

	public static int[] copyUint16ToInt32(char[] data, int len) {
		int[] result = new int[len];
		for (int i = 0; i < len; i++) {
			result[i] = data[i];
		}
		return result;
	}

	public static int[] copyUint8ToInt32(byte[] data, int len) {
		int[] result = new int[len];
		for (int i = 0; i < len; i++) {
			result[i] = Byte.toUnsignedInt(data[i]); 
		}
		return result;
	}

	public static void encodeDelta(short[] data) {
		short prev = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			short curr = data[i];
			data[i] = (short) (curr - prev);
			prev = curr;
		}
	}

	public static void encodeDelta(int[] data) {
		int prev = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			int curr = data[i];
			data[i] = curr - prev;
			prev = curr;
		}
	}

	public static long[] encodeDeltaCopy(long[] data, int len) {
		long[] result = new long[len];
		long prev = 0;
		for (int i = 0; i < len; i++) {
			long curr = data[i];
			result[i] = curr - prev;
			prev = curr;
		}
		return result;
	}

	public static long[] encodeDeltaZigZagCopy(long[] data, int len) {
		long[] result = new long[len];
		long prev = 0;
		for (int i = 0; i < len; i++) {
			long curr = data[i];
			result[i] = encodeZigZag(curr - prev);
			prev = curr;
		}
		return result;
	}

	public static void encodeDeltaZigZag(short[] data) {
		short prev = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			short curr = data[i];
			data[i] = encodeZigZag((short)(curr - prev));
			prev = curr;
		}
	}

	public static void decodeDelta(int[] data) {
		int curr = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			curr += data[i];
			data[i] = curr;
		}
	}

	public static void decodeDelta(short[] data) {
		short curr = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			curr += data[i];
			data[i] = curr;
		}
	}

	public static void decodeDeltaZigZag(int[] data) {
		int curr = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			curr += decodeZigZag(data[i]);
			data[i] = curr;
		}
	}

	public static void decodeDeltaZigZag(short[] data) {
		short curr = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			curr += decodeZigZag(data[i]);
			data[i] = curr;
		}
	}

	public static char[] decodeUint16DeltaZigZagCopy(int[] data) {
		int curr = 0;
		final int SIZE = data.length;
		char[] result = new char[SIZE];
		for (int i = 0; i < SIZE; i++) {
			curr += decodeZigZag(data[i]);
			result[i] = (char) curr;
		}
		return result;
	}

	public static byte[] decodeDeltaCopy(byte[] data, int off, int len) {
		byte curr = 0;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++) {
			curr += data[i + off];
			result[i] = curr;
		}
		return result;
	}

	public static byte[] copyInt32ToUint8(int[] data) {
		final int SIZE = data.length;
		byte[] result = new byte[SIZE];
		for (int i = 0; i < SIZE; i++) {
			result[i] = (byte) data[i];
		}
		return result;
	}

	public static char[] copyInt32ToUint16(int[] data) {
		final int SIZE = data.length;
		char[] result = new char[SIZE];
		for (int i = 0; i < SIZE; i++) {
			result[i] = (char) data[i];
		}
		return result;
	}

	public static byte[] intToByteArray(int[] data) {
		int SIZE_INTS = data.length;
		byte[] result = new byte[SIZE_INTS*4];
		int pos=0;
		for(int i=0; i<SIZE_INTS; i++) {
			int v = data[i];
			result[pos] = (byte) (v);
			result[pos+1] = (byte) (v >> 8);
			result[pos+2] = (byte) (v >> 16);
			result[pos+3] = (byte) (v >> 24);
			pos+=4;
		}
		return result;
	}

	public static byte[] intToByteArray(int[] data, int len) {
		byte[] result = new byte[len << 2];
		int pos=0;
		for(int i = 0; i < len; i++) {
			int v = data[i];
			result[pos] = (byte) (v);
			result[pos+1] = (byte) (v >> 8);
			result[pos+2] = (byte) (v >> 16);
			result[pos+3] = (byte) (v >> 24);
			pos+=4;
		}
		return result;
	}

	public static void intToByteArray(int[] data, int len, byte[] buf, int pos) {
		for(int i = 0; i < len; i++) {
			int v = data[i];
			buf[pos] = (byte) (v);
			buf[pos+1] = (byte) (v >> 8);
			buf[pos+2] = (byte) (v >> 16);
			buf[pos+3] = (byte) (v >> 24);
			pos+=4;
		}
	}

	public static byte[] int64ToByteArray(long[] data, int len) {
		int SIZE = data.length;
		byte[] result = new byte[SIZE * 8];
		int pos = 0;
		for(int i = 0; i < SIZE; i++) {
			long v = data[i];
			result[pos] = (byte) (v);
			result[pos+1] = (byte) (v >> 8);
			result[pos+2] = (byte) (v >> 16);
			result[pos+3] = (byte) (v >> 24);
			result[pos+4] = (byte) (v >> 32);
			result[pos+5] = (byte) (v >> 40);
			result[pos+6] = (byte) (v >> 48);
			result[pos+7] = (byte) (v >> 56);
			pos += 8;
		}
		return result;
	}

	public static void int64ToByteArray(long[] data, int len, byte[] buf, int pos) {
		int SIZE = data.length;
		for(int i = 0; i < SIZE; i++) {
			long v = data[i];
			buf[pos] = (byte) (v);
			buf[pos+1] = (byte) (v >> 8);
			buf[pos+2] = (byte) (v >> 16);
			buf[pos+3] = (byte) (v >> 24);
			buf[pos+4] = (byte) (v >> 32);
			buf[pos+5] = (byte) (v >> 40);
			buf[pos+6] = (byte) (v >> 48);
			buf[pos+7] = (byte) (v >> 56);
			pos += 8;
		}
	}

	public static int[] castShortToInt(short[] data) {
		int len = data.length;
		int[] result = new int[len];
		for (int i = 0; i < len; i++) {
			result[i] = data[i];
		}
		return result;
	}

	public static short[] castIntToShort(int[] data) {
		int len = data.length;
		short[] result = new short[len];
		for (int i = 0; i < len; i++) {
			result[i] = (short) data[i];
		}
		return result;
	}

	public static int[] byteToIntArray(byte[] data) {
		int SIZE_INTS = data.length/4;
		int[] result = new int[SIZE_INTS];
		int pos=0;
		for(int i=0; i<SIZE_INTS; i++) {
			result[i] = (data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24);
			pos+=4;
		}		
		return result;
	}

	public static int[] byteToIntArray(byte[] data, int len) {
		int[] result = new int[len];
		int pos=0;
		for(int i = 0; i < len; i++) {
			result[i] = (data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24);
			pos+=4;
		}		
		return result;
	}

	public static int[] byteToIntArray(byte[] data, int byte_off, int len) {
		int int_len = len >> 2;
			int[] result = new int[int_len];
			int pos = byte_off;
			for(int i = 0; i < int_len; i++) {
				result[i] = (data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24);
				pos+=4;
			}		
			return result;
	}

	public static short[] byteToShortArray(byte[] data) {
		int SIZE_SHORTS = data.length/2;
		short[] result = new short[SIZE_SHORTS];
		int pos=0;
		for(int i = 0; i < SIZE_SHORTS; i++) {
			result[i] = (short) ((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8));
			pos += 2;
		}		
		return result;
	}

	public static char[] byteToCharArray(byte[] data) {
		int len = data.length >> 1;
			char[] result = new char[len];
			int pos=0;
			for(int i = 0; i < len; i++) {
				result[i] = (char) ((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8));
				pos += 2;
			}		
			return result;
	}

	public static char[] byteToCharArray(byte[] data, int off, int len) {
		int size = len >> 1;
				char[] result = new char[size];
				int pos = off;
				for(int i = 0; i < size; i++) {
					result[i] = (char) ((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8));
					pos += 2;
				}		
				return result;
	}

	public static long[] byteToLongArray(byte[] data, int off, int len) {
		int size = len >> 3; 
					long[] result = new long[size];
					int pos = off;
					for(int i = 0; i < size; i++) {		
						result[i] = ((long)data[pos]&0xFF)
								|(((long)data[pos+1]&0xFF)<<8)
								|(((long)data[pos+2]&0xFF)<<16)
								|(((long)data[pos+3]&0xFF)<<24)
								|(((long)data[pos+4]&0xFF)<<32)
								|(((long)data[pos+5]&0xFF)<<40)
								|(((long)data[pos+6]&0xFF)<<48)
								|(((long)data[pos+7])<<56);			
						pos += 8;
					}		
					return result;
	}

	public static void byteToShortArray(byte[] data, short[] target) {
		int SIZE_SHORTS = target.length;
		int pos=0;
		for(int i = 0; i < SIZE_SHORTS; i++) {
			target[i] = (short) ((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8));
			pos += 2;
		}		
	}

	public static short[][] byteToShortArrayArray(byte[] data, int width, int height) {
		short[][] result = new short[height][width];
		int pos=0;
		for(int y = 0; y < height; y++) {
			short[] row = result[y];		
			for(int x = 0; x < width; x++) {
				row[x] = (short) ((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8));
				pos += 2;
			}
		}
		return result;
	}

	public static short[][] byteToShortArrayArrayFlipY(byte[] data, int width, int height) {
		short[][] result = new short[height][width];
		int pos=0;
		for(int y = height - 1; y >= 0; y--) {
			short[] row = result[y];		
			for(int x = 0; x < width; x++) {
				row[x] = (short) ((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8));
				pos += 2;
			}
		}
		return result;
	}

	public static int[][] byteToIntArrayArray(byte[] data, int width, int height) {
		int[][] result = new int[height][width];
		int pos=0;
		for(int y = 0; y < height; y++) {
			int[] row = result[y];		
			for(int x = 0; x < width; x++) {				
				row[x] = (data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24);						
				pos += 4;
			}
		}
		return result;
	}

	public static int[][] byteToIntArrayArrayFlipY(byte[] data, int width, int height) {
		int[][] result = new int[height][width];
		int pos=0;
		for(int y = height - 1; y >= 0; y--) {
			int[] row = result[y];		
			for(int x = 0; x < width; x++) {				
				row[x] = (data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24);						
				pos += 4;
			}
		}
		return result;
	}

	public static float[][] byteToFloatArrayArray(byte[] data, int width, int height) {
		float[][] result = new float[height][width];
		int pos=0;
		for(int y = 0; y < height; y++) {
			float[] row = result[y];		
			for(int x = 0; x < width; x++) {				
				row[x] = Float.intBitsToFloat((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24));						
				pos += 4;
			}
		}
		return result;
	}

	public static float[][] byteToFloatArrayArrayFlipY(byte[] data, int width, int height) {
		float[][] result = new float[height][width];
		int pos=0;
		for(int y = height - 1; y >= 0; y--) {
			float[] row = result[y];		
			for(int x = 0; x < width; x++) {				
				row[x] = Float.intBitsToFloat((data[pos] & 0xFF) | ((data[pos+1] & 0xFF)<<8) | ((data[pos+2] & 0xFF)<<16) | (data[pos+3]<<24));						
				pos += 4;
			}
		}
		return result;
	}

	public static void serializeIntArray(DataOutput out, int[] value) throws IOException {
		out.writeInt(value.length);
		out.write(intToByteArray(value));
	}

	public static int[] deserializeIntArray(DataInput in) throws IOException {		
		final int size = in.readInt();
		byte[] byteArray = new byte[size*4];
		in.readFully(byteArray);
		int[] result = byteToIntArray(byteArray);
		return result;
	}



	public static byte[] intToByteArrayBigEndian(int[] data) {
		int SIZE_INTS = data.length;
		byte[] result = new byte[SIZE_INTS * 4];
		int pos=0;
		for(int i=0; i<SIZE_INTS; i++) {
			int v = data[i];
			result[pos+3] = (byte)  v;
			result[pos+2] = (byte) (v >> 8);
			result[pos+1] = (byte) (v >> 16);
			result[pos] = (byte)   (v >> 24);
			pos+=4;
		}
		return result;
	}

	public static int[] byteToIntArrayBigEndian(byte[] data) {
		int SIZE_INTS = data.length/4;
		int[] result = new int[SIZE_INTS];
		int pos=0;
		for(int i=0; i<SIZE_INTS; i++) {			
			int d = data[pos++];
			int c = data[pos++] & 0xFF;
			int b = data[pos++] & 0xFF;
			int a = data[pos++] & 0xFF;
			result[i] = a | (b<<8) | (c<<16) | (d<<24);
		}
		return result;
	}


	public static byte[] shortToByteArrayBigEndian(short[] data, byte[] target) {
		int SIZE_SHORTS = data.length;
		int SIZE_BYTES = SIZE_SHORTS*2;
		if(target==null||target.length!=SIZE_BYTES) {
			target = new byte[SIZE_BYTES];
		}
		int pos=0;
		for(int i=0; i<SIZE_SHORTS; i++) {
			short v = data[i];
			target[pos+1] = (byte)  v;
			target[pos] = (byte)   (v >> 8);
			pos+=2;
		}
		return target;
	}
	
	public static byte[] shortToDiffByteArrayBigEndian(short[] data, byte[] target) {
		int SIZE_SHORTS = data.length;
		int SIZE_BYTES = SIZE_SHORTS*2;
		if(target==null||target.length!=SIZE_BYTES) {
			target = new byte[SIZE_BYTES];
		}
		int pos = 0;
		int prev = 0;
		for(int i = 0; i < SIZE_SHORTS; i++) {
			int curr = data[i];
			int v = curr - prev;
			prev = curr;
			target[pos+1] = (byte)  v;
			target[pos] = (byte)   (v >> 8);
			pos+=2;
		}
		return target;
	}

	public static byte[] booleanToByteArray(boolean[] data, byte[] target) {
		int SIZE_BOOLS = data.length;
		int SIZE_BYTES = SIZE_BOOLS;
		if(target == null || target.length != SIZE_BYTES) {
			target = new byte[SIZE_BYTES];
		}
		for(int i=0; i < SIZE_BOOLS; i++) {
			target[i] = data[i] ? (byte)1 : (byte)0;			
		}
		return target;
	}

	public static byte[] shortToByteArrayBigEndian(short[] data) {
		int SIZE_SHORTS = data.length;
		byte[] result = new byte[SIZE_SHORTS*2];
		int pos=0;
		for(int i=0; i<SIZE_SHORTS; i++) {
			short v = data[i];
			result[pos+1] = (byte)  v;
			result[pos] = (byte)   (v >> 8);
			pos+=2;
		}
		return result;
	}

	public static byte[] shortToByteArray(short[] data) {
		int SIZE_SHORTS = data.length;
		byte[] result = new byte[SIZE_SHORTS*2];
		int pos = 0;
		for(int i = 0; i < SIZE_SHORTS; i++) {
			short v = data[i];
			result[pos] = (byte) v;
			result[pos + 1] = (byte) (v >> 8);
			pos += 2;
		}
		return result;
	}

	public static byte[] charToByteArray(char[] data, int len) {
		byte[] result = new byte[len << 1];
		int pos=0;
		for(int i = 0; i < len; i++) {
			char v = data[i];
			result[pos++] = (byte)  v;
			result[pos++] = (byte)  (v >> 8);
		}
		return result;
	}


	public static void charToByteArray(char[] data, int len, byte[] buf, int pos) {
		for(int i = 0; i < len; i++) {
			char v = data[i];
			buf[pos++] = (byte)  v;
			buf[pos++] = (byte)  (v >> 8);
		}
	}

	public static byte[] doubleToByteArrayBigEndian(double[] data, byte[] target) {
		int size_data = data.length;
		int size_bytes = size_data * 8;
		if(target == null || target.length != size_bytes) {
			target = new byte[size_bytes];
		}
		int pos=0;
		for(int i = 0; i < size_data; i++) {
			long v = Double.doubleToRawLongBits(data[i]);			
			target[pos + 0] = (byte)(v >>> 56);
			target[pos + 1] = (byte)(v >>> 48);
			target[pos + 2] = (byte)(v >>> 40);
			target[pos + 3] = (byte)(v >>> 32);
			target[pos + 4] = (byte)(v >>> 24);
			target[pos + 5] = (byte)(v >>> 16);
			target[pos + 6] = (byte)(v >>>  8);
			target[pos + 7] = (byte)(v >>>  0);			
			pos += 8;
		}
		return target;
	}

	public static byte[] doubleToByteArrayBigEndian(double[] data, int off, int len, byte[] target) {
		int size_bytes = len * 8;
		if(target == null || target.length != size_bytes) {
			target = new byte[size_bytes];
		}
		int dstPos=0;
		int border = off + len;
		for(int srcPos = off; srcPos < border; srcPos++) {
			long v = Double.doubleToRawLongBits(data[srcPos]);			
			target[dstPos + 0] = (byte)(v >>> 56);
			target[dstPos + 1] = (byte)(v >>> 48);
			target[dstPos + 2] = (byte)(v >>> 40);
			target[dstPos + 3] = (byte)(v >>> 32);
			target[dstPos + 4] = (byte)(v >>> 24);
			target[dstPos + 5] = (byte)(v >>> 16);
			target[dstPos + 6] = (byte)(v >>>  8);
			target[dstPos + 7] = (byte)(v >>>  0);			
			dstPos += 8;
		}
		return target;
	}
	
	public static byte[] floatToByteArrayBigEndian(float[] data, byte[] target) {
		int SIZE_DATA = data.length;
		int SIZE_BYTES = SIZE_DATA * 4;
		if(target == null || target.length != SIZE_BYTES) {
			target = new byte[SIZE_DATA * 4];
		}
		int pos=0;
		for(int i = 0; i < SIZE_DATA; i++) {
			int v = Float.floatToRawIntBits(data[i]);			
			target[pos + 0] = (byte)(v >>> 24);
			target[pos + 1] = (byte)(v >>> 16);
			target[pos + 2] = (byte)(v >>>  8);
			target[pos + 3] = (byte)(v >>>  0);			
			pos += 4;
		}
		return target;
	}

	public static byte[] floatToDiffByteArrayBigEndian(float[] data, byte[] target) {
		int SIZE_DATA = data.length;
		int SIZE_BYTES = SIZE_DATA * 4;
		if(target == null || target.length != SIZE_BYTES) {
			target = new byte[SIZE_DATA * 4];
		}
		int pos=0;
		float prev = 0;
		for(int i = 0; i < SIZE_DATA; i++) {
			float curr = data[i];
			int v = Float.floatToRawIntBits(curr - prev);
			prev = curr;
			target[pos + 0] = (byte)(v >>> 24);
			target[pos + 1] = (byte)(v >>> 16);
			target[pos + 2] = (byte)(v >>>  8);
			target[pos + 3] = (byte)(v >>>  0);			
			pos += 4;
		}
		return target;
	}

	public static byte[] intToByteArrayBigEndian(int[] data, byte[] target) {
		int SIZE_DATA = data.length;
		int SIZE_BYTES = SIZE_DATA * 4;
		if(target == null || target.length != SIZE_BYTES) {
			target = new byte[SIZE_DATA * 4];
		}
		int pos=0;
		for(int i = 0; i < SIZE_DATA; i++) {
			int v = data[i];			
			target[pos + 0] = (byte)(v >>> 24);
			target[pos + 1] = (byte)(v >>> 16);
			target[pos + 2] = (byte)(v >>>  8);
			target[pos + 3] = (byte)(v >>>  0);			
			pos += 4;
		}
		return target;
	}

	public static final Serializer<int[]> INT_ARRAY_compatible_Serializer = new Serializer<int[]>() { //compatible to Serializer.INT_ARRAY

		@Override
		public void serialize(DataOutput out, int[] value) throws IOException {
			DataIO.packInt(out,value.length);
			out.write(intToByteArrayBigEndian(value));			
		}

		@Override
		public int[] deserialize(DataInput in, int available) throws IOException {
			final int size = DataIO.unpackInt(in);
			//Logger.info("deser "+size);
			byte[] byteArray = new byte[size*4];
			in.readFully(byteArray);
			return byteToIntArrayBigEndian(byteArray);
		}

		@Override
		public boolean isTrusted() {
			return true;
		}

		@Override
		public boolean equals(int[] a1, int[] a2) {
			return Arrays.equals(a1,a2);
		}

		@Override
		public int hashCode(int[] bytes, int seed) {
			for (int i : bytes) {
				seed = (-1640531527) * seed + i;
			}
			return seed;
		}

	};

	public static void writeArrayArrayBE(DataOutput out, double[][] data) throws IOException {//Big endian
		byte[] buffer = null;
		for(double[] row:data) {
			buffer = writeArrayBE(out, row, buffer);
		}
	}

	public static byte[] writeArrayBE(DataOutput out, double[] data, byte[] buffer) throws IOException {//Bigendian
		int len = data.length;
		int byteLen = len*8;
		if(buffer==null||buffer.length!=byteLen) {
			buffer = new byte[byteLen];
		}
		for(int i=0;i<len;i++) {
			long v = Double.doubleToRawLongBits(data[i]);
			int b = 8*i;			
			buffer[b+0] = (byte)(v >>> 56);
			buffer[b+1] = (byte)(v >>> 48);
			buffer[b+2] = (byte)(v >>> 40);
			buffer[b+3] = (byte)(v >>> 32);
			buffer[b+4] = (byte)(v >>> 24);
			buffer[b+5] = (byte)(v >>> 16);
			buffer[b+6] = (byte)(v >>>  8);
			buffer[b+7] = (byte)(v >>>  0);
		}
		out.write(buffer);
		return buffer;
	}

	public static byte[] writeArrayBE(DataOutput out, int[] data, byte[] buffer) throws IOException {//Bigendian
		int len = data.length;
		int byteLen = len * 4;
		if(buffer == null || buffer.length != byteLen) {
			buffer = new byte[byteLen];
		}
		for(int i=0;i<len;i++) {
			int v = data[i];
			int b = 4 * i;
			buffer[b+0] = (byte)(v >>> 24);
			buffer[b+1] = (byte)(v >>> 16);
			buffer[b+2] = (byte)(v >>>  8);
			buffer[b+3] = (byte)(v >>>  0);
		}
		out.write(buffer);
		return buffer;
	}



	public static void writeSubArrayArrayBE(DataOutput out, double[][] data, int start_y, int border_y, int start_x, int border_x) throws IOException {//Bigendian
		byte[] buffer = null;
		for (int y = start_y; y < border_y; y++) {
			buffer = writeSubArrayBE(out, data[y], buffer, start_x, border_x);
		}		
	}

	public static byte[] writeSubArrayBE(DataOutput out, double[] data, byte[] buffer, int start_x, int border_x) throws IOException {//Bigendian
		int len = border_x-start_x;
		int byteLen = len*8;
		if(buffer==null||buffer.length!=byteLen) {
			buffer = new byte[byteLen];
		}
		for (int x = start_x; x < border_x; x++) {
			long v = Double.doubleToRawLongBits(data[x]);
			int b = 8*(x-start_x);			
			buffer[b+0] = (byte)(v >>> 56);
			buffer[b+1] = (byte)(v >>> 48);
			buffer[b+2] = (byte)(v >>> 40);
			buffer[b+3] = (byte)(v >>> 32);
			buffer[b+4] = (byte)(v >>> 24);
			buffer[b+5] = (byte)(v >>> 16);
			buffer[b+6] = (byte)(v >>>  8);
			buffer[b+7] = (byte)(v >>>  0);
		}
		out.write(buffer);
		return buffer;
	}

	public static void writeSubArrayArrayBE(DataOutput out, short[][] data, int start_y, int border_y, int start_x, int border_x) throws IOException {//Bigendian
		byte[] buffer = null;
		for (int y = start_y; y < border_y; y++) {
			buffer = writeSubArrayBE(out, data[y], buffer, start_x, border_x);
		}		
	}

	public static void writeSubArrayArrayFlipRowsBE(DataOutput out, short[][] data, int xmin, int ymin, int xmax, int ymax) throws IOException {//Bigendian
		byte[] buffer = null;
		for (int y = ymax; y>=ymin; y--) {
			buffer = writeSubArrayBE(out, data[y], buffer, xmin, xmax+1);
		}		
	}

	public static byte[] writeSubArrayBE(DataOutput out, short[] data, byte[] buffer, int start_x, int border_x) throws IOException {//Bigendian
		int len = border_x-start_x;
		int byteLen = len*8;
		if(buffer==null||buffer.length!=byteLen) {
			buffer = new byte[byteLen];
		}
		for (int x = start_x; x < border_x; x++) {
			short v = data[x];
			int b = 2*(x-start_x);			
			buffer[b] = (byte)   (v >> 8);
			buffer[b+1] = (byte)  v;
		}
		out.write(buffer);
		return buffer;
	}

	public final static ThreadLocal<IntCompressor> THREAD_LOCAL_IC = new ThreadLocal<IntCompressor>() {
		@Override
		protected IntCompressor initialValue() {
			SkippableComposition codec = new SkippableComposition(new FastPFOR128(), new VariableByte());
			IntCompressor ic = new IntCompressor(codec);
			return ic;
		}		
	};

	public static void decodeDelta(long[] data) {
		long curr = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			curr += data[i];
			data[i] = curr;
		}
	}

	public static void decodeDeltaZigZag(long[] data) {
		long curr = 0;
		final int SIZE = data.length;
		for (int i = 0; i < SIZE; i++) {
			curr += decodeZigZag(data[i]);
			data[i] = curr;
		}
	}

	public static void writeIntsWithSize(int[] compressed, ByteBuffer byteBuffer) {
		byteBuffer.putInt(compressed.length);
		for(int c:compressed) {
			byteBuffer.putInt(c);
		}		
	}

	public static int[] readIntsWithSize(ByteBuffer byteBuffer) {
		int len = byteBuffer.getInt();
		int[] data = new int[len];
		for(int i=0; i<len; i++) {
			data[i] = byteBuffer.getInt();
		}
		return data;
	}


	public static void writeInts(int[] compressed, ByteBuffer byteBuffer) {
		byteBuffer.putInt(compressed.length);
		for(int c:compressed) {
			byteBuffer.putInt(c);
		}		
	}
}
