package util;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Writes data to a pre-allocated array.
 * little endian
 * @author woellauer
 *
 */
public class ByteArrayOut extends OutputStream implements RichDataOutput {

	private static final int BYTE_ARRAY_MAX_SIZE = Integer.MAX_VALUE-2;
	private static final int MAX_ADD_SHIFT_1 = 1073741823;
	private static final byte[] EMPTY_ARRAY = {};

	public byte[] buf;
	public int pos;
	
	public static ByteArrayOut of() {
		return new ByteArrayOut();
	}
	
	public static ByteArrayOut of(int size) {
		return new ByteArrayOut(size);
	}
	
	public static ByteArrayOut of(byte[] buf) {
		return new ByteArrayOut(buf);
	}
	
	public static ByteArrayOut of(byte[] buf, int pos) {
		return new ByteArrayOut(buf, pos);
	}

	public ByteArrayOut() {
		this.buf = EMPTY_ARRAY;
		this.pos = 0;
	}

	public ByteArrayOut(int size) {
		this.buf = new byte[size];
		pos = 0;
	}

	public ByteArrayOut(byte[] buf) {
		this.buf = buf;
		pos = 0;
	}

	public ByteArrayOut(byte[] buf, int pos) {
		this.buf = buf;
		this.pos = pos;
	}

	@Override
	public void ensureAvail(int size) {
		ensureCapacity(pos + size);
	}

	public void ensureAvailMax(int size) {
		ensureCapacityMax(pos + size);
	}

	@Override
	public void ensureCapacity(int size) {
		if(buf.length<size) {
			grow(size);
		}		
	}

	public void ensureCapacityMax(int size) {
		if(buf.length<size) {
			growExact(size);
		}		
	}

	/**
	 * for internal use only
	 * @param minSize
	 */
	public void grow(int minSize) {
		int len = buf.length;
		if(minSize>BYTE_ARRAY_MAX_SIZE) {
			throwOverflow(minSize);
		}		
		int newSize = Math.max(minSize, len > MAX_ADD_SHIFT_1 ? BYTE_ARRAY_MAX_SIZE : len + (len >> 1));
		byte[] b = new byte[newSize];
		System.arraycopy(buf, 0, b, 0, len);
		buf = b;
	}

	private void growExact(int size) {
		if(size>BYTE_ARRAY_MAX_SIZE) {
			throwOverflow(size);
		}		
		byte[] b = new byte[size];
		System.arraycopy(buf, 0, b, 0, buf.length);
		buf = b;
	}

	public void trim() {
		int p = pos;
		if(p == 0) {
			buf = EMPTY_ARRAY;
		}
		byte[] bb = buf;
		if(p < bb.length) {
			byte[] b = new byte[p];
			System.arraycopy(bb, 0, b, 0, p);
			buf = b;
		}
	}

	private void throwOverflow(int size) {
		throw new RuntimeException("overflow of "+size+" maximum is "+BYTE_ARRAY_MAX_SIZE);
	}

	@Override
	public void putByte(byte v) {
		putByte(pos++, v);
	}

	@Override
	public void putByteRaw(byte v) {
		buf[pos++] = v;		
	}	

	@Override
	public void putByteRaw(int p, byte v) {
		buf[p] = v;		
	}

	@Override
	public void putShort(short v) {
		int p = pos;
		putShort(p, v);
		pos = p+2;
	}

	@Override
	public void putShortRaw(short v) {
		int p = pos;
		putShortRaw(p, v);		
		pos = p+2;
	}	

	@Override
	public void putShortRaw(int p, short v) {
		byte[] b = buf;
		b[p] = (byte) (v);
		b[p+1] = (byte) (v >> 8);
	}	

	@Override
	public void putInt(int v) {
		int p = pos;
		putInt(p, v);
		pos = p+4;
	}

	@Override
	public void putIntRaw(int v) {
		int p = pos;
		putIntRaw(p, v);		
		pos = p+4;
	}	

	@Override
	public void putIntRaw(int p, int v) {
		byte[] b = buf;
		b[p] = (byte) (v);
		b[p+1] = (byte) (v >> 8);
		b[p+2] = (byte) (v >> 16);
		b[p+3] = (byte) (v >> 24);
	}

	@Override
	public void putLong(long v) {
		int p = pos;
		putLong(p, v);
		pos = p+8;		
	}

	@Override
	public void putLongRaw(long v) {
		int p = pos;
		putLongRaw(p, v);		
		pos = p+8;		
	}

	@Override
	public void putLongRaw(int p, long v) {
		byte[] b = buf;
		b[p] = (byte) (v);
		b[p+1] = (byte) (v >> 8);
		b[p+2] = (byte) (v >> 16);
		b[p+3] = (byte) (v >> 24);		
		b[p+4] = (byte) (v >> 32);
		b[p+5] = (byte) (v >> 40);
		b[p+6] = (byte) (v >> 48);
		b[p+7] = (byte) (v >> 56);		
	}

	public void putInts(int[] vs) {
		int p = pos;
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putIntRaw(p+i,vs[i]);
		}
		pos = p+len;
	}

	@Override
	public void putBytes(byte[] v, int offset, int len) {
		int p = pos;
		putBytes(p, v, offset, len);
		pos = p+len;		
	}	

	@Override
	public void putBytesRaw(byte[] v, int offset, int len) {
		int p = pos;
		System.arraycopy(v, offset, buf, p, len);	
		pos = p+len;		
	}

	@Override
	public void putBytesRaw(int p, byte[] v, int offset, int len) {
		System.arraycopy(v, offset, buf, p, len);
	}

	public void putIntsRaw(int[] v) {
		putIntsBorderedRaw(v, 0, v.length);
	}

	public void putIntsRaw(int[] v, int len) {
		putIntsBorderedRaw(v, 0, len);
	}

	public void putIntsRaw(int[] v, int offset, int len) {
		putIntsBorderedRaw(v, offset, offset + len);
	}

	public void putIntsBorderedRaw(int[] v, int offset, int border) {
		int p = pos;
		for(int i=offset; i<border; i++) {
			putIntRaw(p, v[i]);
			p += 4;
		}
		pos = p;
	}

	public void putIntsRaw(int p, int[] v) {
		int len = v.length;
		for(int i=0; i<len; i++) {
			putIntRaw(p, v[i]);
			p += 4;
		}
	}

	public void putIntsRaw(int p, int[] v, int len) {
		for(int i=0; i<len; i++) {
			putIntRaw(p, v[i]);
			p += 4;
		}
	}

	public void putIntsRaw(int p, int[] v, int offset, int len) {
		putIntsBorderedRaw(p, v, offset, offset + len);
	}

	public void putIntsBorderedRaw(int p, int[] v, int offset, int border) {
		for(int i=offset; i<border; i++) {
			putIntRaw(p, v[i]);
			p += 4;
		}
	}

	public void reset() {
		pos = 0;
	}

	public void resetZero() {
		buf = EMPTY_ARRAY;
		pos = 0;
	}

	public void putDoubles(double[] vs) {
		putDoublesBordered(vs, 0, vs.length);
	}

	public void putDoubles(double[] vs, int len) {
		putDoublesBordered(vs, 0, len);
	}

	public void putDoubles(double[] vs, int offset, int len) {
		putDoublesBordered(vs, offset, offset + len);
	}

	public void putDoublesBordered(double[] vs, int offset, int border) {
		ensureCapacity((border-offset)*8);
		byte[] b = buf;
		int p = pos;
		for (int i = offset; i < border; i++) {
			long v = Double.doubleToRawLongBits(vs[i]);			
			b[p++] = (byte) (v);
			b[p++] = (byte) (v >> 8);
			b[p++] = (byte) (v >> 16);
			b[p++] = (byte) (v >> 24);		
			b[p++] = (byte) (v >> 32);
			b[p++] = (byte) (v >> 40);
			b[p++] = (byte) (v >> 48);
			b[p++] = (byte) (v >> 56);
		}
		pos = p;
	}

	public void putDoublesRaw(double[] vs) {
		putDoublesBorderedRaw(vs, 0, vs.length);
	}

	public void putDoublesRaw(double[] vs, int len) {
		putDoublesBorderedRaw(vs, 0, len);
	}

	public void putDoublesRaw(double[] vs, int offset, int len) {
		putDoublesBorderedRaw(vs, offset, offset + len);
	}

	public void putDoublesBorderedRaw(double[] vs, int offset, int border) {
		byte[] b = buf;
		int p = pos;
		for (int i = offset; i < border; i++) {
			long v = Double.doubleToRawLongBits(vs[i]);			
			b[p++] = (byte) (v);
			b[p++] = (byte) (v >> 8);
			b[p++] = (byte) (v >> 16);
			b[p++] = (byte) (v >> 24);		
			b[p++] = (byte) (v >> 32);
			b[p++] = (byte) (v >> 40);
			b[p++] = (byte) (v >> 48);
			b[p++] = (byte) (v >> 56);
		}
		pos = p;
	}

	public void putFloats(float[] vs) {
		putFloatsBordered(vs, 0, vs.length);
	}

	public void putFloats(float[] vs, int len) {
		putFloatsBordered(vs, 0, len);
	}

	public void putFloats(float[] vs, int offset, int len) {
		putFloatsBordered(vs, offset, offset + len);
	}

	public void putFloatsBordered(float[] vs, int offset, int border) {
		ensureCapacity((border-offset)*4);
		byte[] b = buf;
		int p = pos;
		for (int i = offset; i < border; i++) {
			int v = Float.floatToRawIntBits(vs[i]);			
			b[p++] = (byte) (v);
			b[p++] = (byte) (v >> 8);
			b[p++] = (byte) (v >> 16);
			b[p++] = (byte) (v >> 24);
		}
		pos = p;
	}

	public void putFloatsRaw(float[] vs) {
		putFloatsBorderedRaw(vs, 0, vs.length);
	}

	public void putFloatsRaw(float[] vs, int len) {
		putFloatsBorderedRaw(vs, 0, len);
	}

	public void putFloatsRaw(float[] vs, int offset, int len) {
		putFloatsBorderedRaw(vs, offset, offset + len);
	}

	public void putFloatsBorderedRaw(float[] vs, int offset, int border) {
		byte[] b = buf;
		int p = pos;
		for (int i = offset; i < border; i++) {
			int v = Float.floatToRawIntBits(vs[i]);			
			b[p++] = (byte) (v);
			b[p++] = (byte) (v >> 8);
			b[p++] = (byte) (v >> 16);
			b[p++] = (byte) (v >> 24);
		}
		pos = p;
	}

	public void putFloats(double[] vs) {
		putFloatsBordered(vs, 0, vs.length);
	}

	public void putFloats(double[] vs, int len) {
		putFloatsBordered(vs, 0, len);
	}

	public void putFloats(double[] vs, int offset, int len) {
		putFloatsBordered(vs, offset, offset + len);
	}

	public void putFloatsBordered(double[] vs, int offset, int border) {
		ensureCapacity((border-offset)*4);
		byte[] b = buf;
		int p = pos;
		for (int i = offset; i < border; i++) {
			int v = Float.floatToRawIntBits((float)vs[i]);			
			b[p++] = (byte) (v);
			b[p++] = (byte) (v >> 8);
			b[p++] = (byte) (v >> 16);
			b[p++] = (byte) (v >> 24);
		}
		pos = p;
	}

	public void putFloatsRaw(double[] vs) {
		putFloatsBorderedRaw(vs, 0, vs.length);
	}

	public void putFloatsRaw(double[] vs, int len) {
		putFloatsBorderedRaw(vs, 0, len);
	}

	public void putFloatsRaw(double[] vs, int offset, int len) {
		putFloatsBorderedRaw(vs, offset, offset + len);
	}

	public void putFloatsBorderedRaw(double[] vs, int offset, int border) {
		byte[] b = buf;
		int p = pos;
		for (int i = offset; i < border; i++) {
			int v = Float.floatToRawIntBits((float)vs[i]);			
			b[p++] = (byte) (v);
			b[p++] = (byte) (v >> 8);
			b[p++] = (byte) (v >> 16);
			b[p++] = (byte) (v >> 24);
		}
		pos = p;
	}

	public void putFloats2dBorderedRaw(double[][] data, int rowOffset, int rowBorder, int colOffset, int colBorder) {
		byte[] b = buf;
		int p = pos;
		for (int y = rowOffset; y < rowBorder; y++) {
			double[] row = data[y];
			for (int x = colOffset; x < colBorder; x++) {
				int v = Float.floatToRawIntBits((float)row[x]);			
				b[p++] = (byte) (v);
				b[p++] = (byte) (v >> 8);
				b[p++] = (byte) (v >> 16);
				b[p++] = (byte) (v >> 24);
			}
		}
		pos = p;
	}

	public int flip(OutputStream out) throws IOException {
		int p = pos;
		out.write(buf, 0, p);
		pos = 0;
		return p;
	}

	public int flip(DataOutput out) throws IOException {
		int p = pos;
		out.write(buf, 0, p);
		pos = 0;
		return p;
	}

	public int flip(ByteBuffer out) {
		int p = pos;
		out.put(buf, 0, p);
		pos = 0;
		return p;
	}

	public int flip(WritableByteChannel out) throws IOException  {// TODO check
		int p = pos;
		ByteBuffer byteBuffer = ByteBuffer.wrap(buf, 0, p);
		out.write(byteBuffer);
		pos = 0;
		return p;
	}

	public int flip(byte[] out) {
		return flip(out, 0);
	}

	public int flip(byte[] out, int offset) {
		int p = pos;
		System.arraycopy(buf, 0, out, offset, p);
		pos = 0;
		return p;
	}
	
	public ByteArrayOutStream toOutputStream() {
		return new ByteArrayOutStream(this);
	}

	@Override
	public void write(int b) throws IOException {
		putByte((byte) b);		
	}

	@Override
	public void write(byte[] b) throws IOException {
		putBytes(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		putBytes(b, off, len);
	}
}