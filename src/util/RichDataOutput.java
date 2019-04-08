package util;

import java.io.DataOutput;
import java.io.IOException;

public interface RichDataOutput extends DataOutput {

	void ensureCapacity(int i);
	void ensureAvail(int size);

	void putByte(byte v);
	void putByteRaw(byte v);
	void putByteRaw(int p, byte v);

	void putBytes(byte[] v, int offset, int len);
	void putBytesRaw(byte[] v, int offset, int len);
	void putBytesRaw(int p, byte[] v, int offset, int len);

	void putShort(short v);
	void putShortRaw(short v);
	void putShortRaw(int p, short v);

	void putInt(int v);
	void putIntRaw(int v);
	void putIntRaw(int p, int v);

	void putLong(long v);
	void putLongRaw(long v);
	void putLongRaw(int p, long v);

	default void putByte(int p, byte v) {
		ensureCapacity(p+1);
		putByteRaw(p, v);	
	}

	default void putBytes(byte[] v) {
		putBytes(v, 0, v.length);
	}	

	default void putBytes(byte[] v, int len) {
		putBytes(v, 0, len);
	}	

	default void putBytes(int p, byte[] v) {
		int len = v.length;
		ensureCapacity(p+len);
		putBytesRaw(p, v, 0, len);
	}

	default void putBytes(int p, byte[] v, int len) {
		ensureCapacity(p+len);
		putBytesRaw(p, v, 0, len);
	}

	default void putBytes(int p, byte[] v, int offset, int len) {
		ensureCapacity(p+len);
		putBytesRaw(p, v, offset, len);
	}

	default void putBytesRaw(byte[] v) {
		putBytesRaw(v, 0, v.length);		
	}

	default void putBytesRaw(byte[] v, int len) {
		putBytesRaw(v, 0, len);	
	}

	default void putBytesRaw(int p, byte[] v) {
		putBytesRaw(p, v, 0, v.length);
	}

	default void putBytesRaw(int p, byte[] v, int len) {
		putBytesRaw(p, v, 0, len);
	}

	default void putInt(int p, int v) {
		ensureCapacity(p+4);
		putIntRaw(p, v);
	}

	default void putLong(int p, long v) {
		ensureCapacity(p+8);
		putLongRaw(p, v);		
	}

	default void putShort(int p, short v) {
		ensureCapacity(p+2);
		putShortRaw(p, v);		
	}

	default void putBoolean(boolean v) {
		putByte(v?(byte)1:(byte)0);
	}

	default void putBooleanRaw(boolean v) {
		putByteRaw(v?(byte)1:(byte)0);
	}

	default void putBoolean(int p, boolean v) {
		putByte(p, v?(byte)1:(byte)0);
	}

	default void putBooleanRaw(int p, boolean v) {
		putByteRaw(p, v?(byte)1:(byte)0);
	}

	default void putChar(char v) {
		putShort((short) v);
	}

	default void putCharRaw(char v) {
		putShortRaw((short) v);
	}

	default void putChar(int p, char v) {
		putShort(p, (short) v);
	}

	default void putCharRaw(int p, char v) {
		putShortRaw(p, (short) v);
	}

	default void putFloat(float v) {
		putInt(Float.floatToRawIntBits(v));
	}

		default void putFloat(double v) {
		putInt(Float.floatToRawIntBits((float)v));
	}

	default void putFloatRaw(float v) {
		putIntRaw(Float.floatToRawIntBits(v));		
	}

	default void putFloatRaw(double v) {
		putIntRaw(Float.floatToRawIntBits((float)v));		
	}

	default void putFloat(int p, float v) {
		putInt(p, Float.floatToRawIntBits(v));		
	}

	default void putFloatRaw(int p, float v) {
		putIntRaw(p, Float.floatToRawIntBits(v));		
	}

	default void putDouble(double v) {
		putLong(Double.doubleToRawLongBits(v));
	}

	default void putDoubleRaw(double v) {
		putLongRaw(Double.doubleToRawLongBits(v));		
	}

	default void putDouble(int p, double v) {
		putLong(p, Double.doubleToRawLongBits(v));		
	}

	default void putDoubleRaw(int p, double v) {
		putLongRaw(p, Double.doubleToRawLongBits(v));		
	}

	@Override
	default void writeBoolean(boolean v) throws IOException {
		putBoolean(v);		
	}
	@Override
	default void writeByte(int v) throws IOException {
		putByte((byte) v);		
	}
	@Override
	default void writeShort(int v) throws IOException {
		putShort((short)v);		
	}
	@Override
	default void writeChar(int v) throws IOException {
		putChar((char) v);		
	}
	@Override
	default void writeInt(int v) throws IOException {
		putInt(v);		
	}
	@Override
	default void writeLong(long v) throws IOException {
		putLong(v);		
	}
	@Override
	default void writeFloat(float v) throws IOException {
		putFloat(v);		
	}
	@Override
	default void writeDouble(double v) throws IOException {
		putDouble(v);		
	}
	@Override
	default void writeBytes(String s) throws IOException {
		throw new RuntimeException();		
	}
	@Override
	default void writeChars(String s) throws IOException {
		throw new RuntimeException();			
	}
	@Override
	default void writeUTF(String s) throws IOException {
		throw new RuntimeException();		
	}
}