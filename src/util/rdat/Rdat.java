package util.rdat;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import util.Serialisation;

public class Rdat {
	
	public final static int TYPE_SIGNED_INT = 1;
	public final static int TYPE_UNSIGNED_INT = 2;
	public final static int TYPE_FLOAT = 3;
	public final static int TYPE_STRING = 4;
	public static final int TYPE_STRING_SIZE = 0xff; // no size	
	public final static int TYPE_BASIC_OBJECT = 5;
	public final static int TYPE_BASIC_OBJECT_SIZE = 4;
	public final static int TYPE_LOGICAL = 6;
	public final static int TYPE_LOGICAL_SIZE = 1;

	public final static int TYPE_INT32 = TYPE_SIGNED_INT;  // unsigned 32 bit not supported in R
	public final static int TYPE_INT32_SIZE = 4;
	public final static int TYPE_INT16 = TYPE_SIGNED_INT;
	public final static int TYPE_INT16_SIZE = 2;
	public final static int TYPE_UINT16 = TYPE_UNSIGNED_INT;
	public final static int TYPE_UINT16_SIZE = 2;
	public final static int TYPE_UINT8 = TYPE_UNSIGNED_INT;
	public final static int TYPE_UINT8_SIZE = 1;
	public final static int TYPE_INT8 = TYPE_SIGNED_INT;
	public final static int TYPE_INT8_SIZE = 1;
	public final static int TYPE_FLOAT32 = TYPE_FLOAT;
	public final static int TYPE_FLOAT32_SIZE = 4;
	public final static int TYPE_FLOAT64 = TYPE_FLOAT;
	public final static int TYPE_FLOAT64_SIZE = 8;
	public final static int TYPE_INT64 = TYPE_SIGNED_INT; // unsigned 64 bit not supported in R
	public final static int TYPE_INT64_SIZE = 8;
	
	public static final byte[] SIGNATURE_RDAT = stringToBytes("RDAT");
	public static final byte[] SIGNATURE_LIST = stringToBytes("LIST");
	public static final byte[] SIGNATURE_VECT = stringToBytes("VECT");
	public static final byte[] SIGNATURE_MTRX = stringToBytes("MTRX");
	public static final byte[] SIGNATURE_DTFM = stringToBytes("DTFM");
	public static final byte[] SIGNATURE_VDIM = stringToBytes("VDIM");
	
	public static final byte[] RDAT_TYPE_RASTER = stringToSizedBytes("RASTER");
	public static final byte[] RDAT_TYPE_POINT_DATA_FRAME = stringToSizedBytes("POINT_DATA_FRAME");
	public static final byte[] RDAT_TYPE_DATA_FRAME = stringToSizedBytes("DATA_FRAME");
	public static final byte[] RDAT_TYPE_DIM_VECTOR = stringToSizedBytes("DIM_VECTOR");


	private static byte[] stringToBytes(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}
	
	private static byte[] stringToSizedBytes(String text) {
		byte[] bytes = stringToBytes(text);
		if(bytes.length>255) {
			throw new RuntimeException("text too big: "+text);
		}
		byte[] result = new byte[bytes.length+1];
		result[0] = (byte) bytes.length;
		System.arraycopy(bytes, 0, result, 1, bytes.length);
		return result;
	}	
	
	public static void writeSizedString(DataOutput out, String text) throws IOException {
		out.write(stringToSizedBytes(text));
	}	
	public static void writeShortArrayArray(DataOutput out, short[][] data) throws IOException {
		byte[] buffer = null;
		for(short[] row:data) {
			buffer = writeShortArray(out, row, buffer);
		}
	}
	
	private static byte[] writeShortArray(DataOutput out, short[] data, byte[] buffer) throws IOException {//Bigendian
		int len = data.length;
		int byteLen = len*2;
		if(buffer==null||buffer.length!=byteLen) {
			buffer = new byte[byteLen];
		}
		for(int i=0;i<len;i++) {
			buffer[2*i] = (byte) (data[i] >> 8);
			buffer[2*i+1] = (byte) data[i];
		}
		out.write(buffer);
		return buffer;
	}

	public static void write_RDAT_MTRX(DataOutput out, double[][] data) throws IOException {
		out.write(SIGNATURE_MTRX);
		out.writeByte(TYPE_FLOAT64);
		out.writeByte(TYPE_FLOAT64_SIZE);
		out.writeInt(data.length);
		out.writeInt(data[0].length);
		Serialisation.writeArrayArrayBE(out, data);
	}
	
	public static void write_RDAT_MTRX_uint16(DataOutput out, short[][] data) throws IOException {
		out.write(SIGNATURE_MTRX);
		out.writeByte(TYPE_UINT16);
		out.writeByte(TYPE_UINT16_SIZE);
		out.writeInt(data.length);
		out.writeInt(data[0].length);
		writeShortArrayArray(out, data);
	}
	
	public static void write_RDAT_VECT(DataOutput out, double[][] data) throws IOException {
		out.write(SIGNATURE_VECT);
		out.writeByte(TYPE_FLOAT64);
		out.writeByte(TYPE_FLOAT64_SIZE);
		out.writeInt(data[0].length*data.length);
		Serialisation.writeArrayArrayBE(out, data);		
	}
	
	public static void write_RDAT_VECT3(DataOutput out, int x1, int x2, int x3) throws IOException {
		out.write(SIGNATURE_VECT);
		out.writeByte(TYPE_INT32);
		out.writeByte(TYPE_INT32_SIZE);
		out.writeInt(3);
		out.writeInt(x1);
		out.writeInt(x2);
		out.writeInt(x3);	
	}
	
	public static void write_RDAT_VDIM_uint8(DataOutput out, byte[] data, int xlen, int ylen, int zlen) throws IOException {
		out.write(SIGNATURE_VDIM);
		out.writeByte(TYPE_UINT8);
		out.writeByte(TYPE_UINT8_SIZE);
		write_RDAT_VECT3(out, xlen, ylen, zlen);
		int len = xlen * ylen * zlen;
		out.write(data, 0, len);
	}
	
	public static void write_RDAT_VDIM_bool8(DataOutput out, byte[] data, int xlen, int ylen, int zlen) throws IOException {
		out.write(SIGNATURE_VDIM);
		out.writeByte(TYPE_LOGICAL);
		out.writeByte(TYPE_LOGICAL_SIZE);
		write_RDAT_VECT3(out, xlen, ylen, zlen);
		int len = xlen * ylen * zlen;
		out.write(data, 0, len);
	}
	
	public static void write_RDAT_VDIM_uint16(DataOutput out, byte[] data, int xlen, int ylen, int zlen) throws IOException {
		out.write(SIGNATURE_VDIM);
		out.writeByte(TYPE_UINT16);
		out.writeByte(TYPE_UINT16_SIZE);
		write_RDAT_VECT3(out, xlen, ylen, zlen);
		int len = xlen * ylen * zlen * 2;
		out.write(data, 0, len);
	}
	
	public static void write_RDAT_VDIM_int32(DataOutput out, byte[] data, int xlen, int ylen, int zlen) throws IOException {
		out.write(SIGNATURE_VDIM);
		out.writeByte(TYPE_INT32);
		out.writeByte(TYPE_INT32_SIZE);
		write_RDAT_VECT3(out, xlen, ylen, zlen);
		int len = xlen * ylen * zlen * 4;
		out.write(data, 0, len);
	}
	
	public static void write_RDAT_VDIM_int32(DataOutput out, int[] data, int xlen, int ylen, int zlen) throws IOException {
		out.write(SIGNATURE_VDIM);
		out.writeByte(TYPE_INT32);
		out.writeByte(TYPE_INT32_SIZE);
		write_RDAT_VECT3(out, xlen, ylen, zlen);
		int len = xlen * ylen * zlen;
		Serialisation.writeArrayBE(out, data, null);
	}
	
	public static void write_RDAT_VDIM_float32(DataOutput out, byte[] data, int xlen, int ylen, int zlen) throws IOException {
		out.write(SIGNATURE_VDIM);
		out.writeByte(TYPE_FLOAT32);
		out.writeByte(TYPE_FLOAT32_SIZE);
		write_RDAT_VECT3(out, xlen, ylen, zlen);
		int len = xlen * ylen * zlen * 4;
		out.write(data, 0, len);
	}
}
