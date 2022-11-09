package util;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public class BufferedArrayDataOutput implements DataOutput, AutoCloseable {

	//private static final int BYTE_ARRAY_MAX_SIZE = Integer.MAX_VALUE - 2;
	private static final int BYTE_ARRAY_MAX_SIZE = Integer.MAX_VALUE - 8;
	private static final int MAX_ADD_SHIFT_1 = 1073741823;
	private static final int INITIAL_SIZE = 4096;

	public byte[] buf;
	public int pos;

	private final OutputStream out;

	public BufferedArrayDataOutput(OutputStream out) {
		this.out = out;
		this.buf = new byte[INITIAL_SIZE];
		this.pos = 0;
	}

	public void flush() throws IOException {
		out.write(buf, 0, pos);
		pos = 0;
	}

	@Override
	public void close() throws Exception {
		flush();		
	}

	public void ensure(int capacity) throws IOException {
		if(buf.length < ((long) pos) + capacity) {
			growOrFlush(capacity);
		}		
	}
	
	public void ensureExact(int capacity) throws IOException {
		if(buf.length < ((long) pos) + capacity) {
			growOrFlushExact(capacity);
		}		
	}

	private void growOrFlush(int capacity) throws IOException {
		long minSize = ((long) pos) + capacity;
		if(minSize > BYTE_ARRAY_MAX_SIZE) {
			flush();
			minSize = capacity;
		}
		growIfNeeded((int) minSize);			
	}
	
	private void growOrFlushExact(int capacity) throws IOException {
		long minSize = ((long) pos) + capacity;
		if(minSize > BYTE_ARRAY_MAX_SIZE) {
			flush();
			minSize = capacity;
		}
		growIfNeededExact((int) minSize);			
	}

	private void growOrFlush1() throws IOException {
		int minSize = pos + 1;
		if(minSize > BYTE_ARRAY_MAX_SIZE) {
			flush();
			minSize = 1;
		} else {
			growIfNeeded(minSize);
		}
	}
	
	private void growOrFlush2() throws IOException {
		int minSize = pos + 2;
		if(minSize > BYTE_ARRAY_MAX_SIZE) {
			flush();
			minSize = 2;
		} else {
			growIfNeeded(minSize);
		}
	}
	
	private void growOrFlush4() throws IOException {
		int minSize = pos + 4;
		if(minSize > BYTE_ARRAY_MAX_SIZE) {
			flush();
			minSize = 4;
		} else {
			growIfNeeded(minSize);
		}
	}
	
	private void growOrFlush8() throws IOException {
		int minSize = pos + 8;
		if(minSize > BYTE_ARRAY_MAX_SIZE) {
			flush();
			minSize = 8;
		} else {
			growIfNeeded(minSize);
		}
	}

	private void growIfNeeded(int minSize) {
		int len = buf.length;
		if(len < minSize) {
			int newSize = Math.max(minSize, len > MAX_ADD_SHIFT_1 ? BYTE_ARRAY_MAX_SIZE : len + (len >> 1));
			byte[] b = new byte[newSize];
			System.arraycopy(buf, 0, b, 0, len);
			buf = b;
		}	
	}
	
	private void growIfNeededExact(int maxSize) {
		int len = buf.length;
		if(len < maxSize) {
			byte[] b = new byte[maxSize];
			System.arraycopy(buf, 0, b, 0, len);
			buf = b;
		}	
	}

	public void putByteRaw(byte v) {
		buf[pos++] = v;
	}
	
	public void putByte(byte v) throws IOException {
		if(buf.length == pos) {
			growOrFlush1();
		}
		buf[pos++] = v;
	}
	
	@Override
	public void writeByte(int b) throws IOException {
		if(buf.length == pos) {
			growOrFlush1();
		}
		buf[pos++] = (byte) b;			
	}
	
	@Override
	public void write(int b) throws IOException {
		if(buf.length == pos) {
			growOrFlush1();
		}
		buf[pos++] = (byte) b;	
	}

	public void putBoolean(boolean v) throws IOException {
		if(buf.length == pos) {
			growOrFlush1();
		}
		buf[pos++] = (byte) (v ? 1 : 0);
	}
	
	@Override
	public void writeBoolean(boolean v) throws IOException {
		if(buf.length == pos) {
			growOrFlush1();
		}
		buf[pos++] = (byte) (v ? 1 : 0);		
	}

	public void putBooleanRaw(boolean v) {
		buf[pos++] = (byte) (v ? 1 : 0);
	}

	public void putShortRaw(short v) {
		buf[pos++] = (byte) (0xff & (v >> 8));
		buf[pos++] = (byte) (0xff & (v));
	}
	
	public void putShort(short v) throws IOException {
		if(buf.length < pos + 2) {
			growOrFlush2();
		}
		buf[pos++] = (byte) (0xff & (v >> 8));
		buf[pos++] = (byte) (0xff & (v));
	}
	
	@Override
	public void writeShort(int v) throws IOException {
		if(buf.length < pos + 2) {
			growOrFlush2();
		}
		buf[pos++] = (byte) (0xff & (v >> 8));
		buf[pos++] = (byte) (0xff & (v));		
	}
	
	public void putCharRaw(char v) {
		buf[pos++] = (byte) (0xff & (v >> 8));
		buf[pos++] = (byte) (0xff & (v));
	}
	
	public void putChar(char v) throws IOException {
		if(buf.length < pos + 2) {
			growOrFlush2();
		}
		buf[pos++] = (byte) (0xff & (v >> 8));
		buf[pos++] = (byte) (0xff & (v));
	}
	
	@Override
	public void writeChar(int v) throws IOException {
		if(buf.length < pos + 2) {
			growOrFlush2();
		}
		buf[pos++] = (byte) (0xff & (v >> 8));
		buf[pos++] = (byte) (0xff & (v));			
	}
	
	public void putIntRaw(int v) {
		buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	public void putInt(int v) throws IOException {
		if(buf.length < pos + 4) {
			growOrFlush4();
		}
		buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	@Override
	public void writeInt(int v) throws IOException {
		if(buf.length < pos + 4) {
			growOrFlush4();
		}
		buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));	
	}
	
	public void putFloatRaw(float f) {
		int v = Float.floatToRawIntBits(f);
		buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	public void putFloat(float f) throws IOException {
		if(buf.length < pos + 4) {
			growOrFlush4();
		}
		int v = Float.floatToRawIntBits(f);
		buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	@Override
	public void writeFloat(float f) throws IOException {
		if(buf.length < pos + 4) {
			growOrFlush4();
		}
		int v = Float.floatToRawIntBits(f);
		buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));			
	}
	
	public void putLongRaw(long v) {
		buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	public void putLong(long v) throws IOException {
		if(buf.length < pos + 8) {
			growOrFlush8();
		}
		buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	@Override
	public void writeLong(long v) throws IOException {
		if(buf.length < pos + 8) {
			growOrFlush8();
		}
		buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));		
	}
	
	public void putDoubleRaw(double d) {
		long v = Double.doubleToRawLongBits(d);
		buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	public void putDouble(double d) throws IOException {
		if(buf.length < pos + 8) {
			growOrFlush8();
		}
		long v = Double.doubleToRawLongBits(d);
		buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
	}
	
	@Override
	public void writeDouble(double d) throws IOException {
		if(buf.length < pos + 8) {
			growOrFlush8();
		}
		long v = Double.doubleToRawLongBits(d);
		buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));		
	}

	@Override
	public void write(byte[] b) throws IOException {
		throw new RuntimeException("not implemented");		
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		throw new RuntimeException("not implemented");			
	}

	@Override
	public void writeBytes(String s) throws IOException {
		throw new RuntimeException("not implemented");			
	}

	@Override
	public void writeChars(String s) throws IOException {
		throw new RuntimeException("not implemented");			
	}

	@Override
	public void writeUTF(String s) throws IOException {
		throw new RuntimeException("not implemented");			
	}	
}
