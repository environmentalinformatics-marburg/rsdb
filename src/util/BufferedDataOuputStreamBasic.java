package util;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public abstract class BufferedDataOuputStreamBasic extends OutputStream implements DataOutput {
	
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	//private static final int DEFAULT_BUFFER_SIZE = 65536;

	public byte[] buf;
	public int pos;

	private final OutputStream out;

	public BufferedDataOuputStreamBasic(OutputStream out) {
		this.buf = new byte[DEFAULT_BUFFER_SIZE];
		this.pos = 0;
		this.out = out;
	}

	public BufferedDataOuputStreamBasic(OutputStream out, int size) {
		this.buf = new byte[size];
		this.pos = 0;
		this.out = out;
	}
	
	public void ensure(int size) throws IOException {
		if(buf.length < ((long)pos) + size) {
			flushAndEnsure(size);
		}		
	}
	
	public void flushAndEnsure(int size) throws IOException {
		if(pos != 0) {
			flushAlways();
		}
		if(buf.length < size) {
			buf = new byte[size];
		}	
	}

	public void flushAlways() throws IOException {
		out.write(buf, 0, pos);
		pos = 0;
		//out.flush();
	}

	@Override
	public void flush() throws IOException {
		if(pos != 0) {
			out.write(buf, 0, pos);
			pos = 0;
		}
		//out.flush();
	}

	@Override
	public void close() throws IOException {
		flush();
		//out.flush();
		//out.close();
	}

	public void putByte(byte v) throws IOException {
		if(buf.length == pos) {
			flushAlways();
		}
		buf[pos++] = v;
	}

	@Override
	public void writeByte(int b) throws IOException {
		if(buf.length == pos) {
			flushAlways();
		}
		buf[pos++] = (byte) b;			
	}

	@Override
	public void write(int b) throws IOException {
		if(buf.length == pos) {
			flushAlways();
		}
		buf[pos++] = (byte) b;	
	}

	public void putBoolean(boolean v) throws IOException {
		if(buf.length == pos) {
			flushAlways();
		}
		buf[pos++] = (byte) (v ? 1 : 0);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		if(buf.length == pos) {
			flushAlways();
		}
		buf[pos++] = (byte) (v ? 1 : 0);		
	}
	
	private void putBytesBuffer(byte[] b) throws IOException {
		int len = b.length;
		if(buf.length < pos + len) {
			flushAlways();
		}
		System.arraycopy(b, 0, buf, pos, len);
		pos += len;
	}

	public void putBytesDirect(byte[] b) throws IOException {
		if(pos != 0) {
			flushAlways();
		}
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		if(b.length <= buf.length >> 1) {
			putBytesBuffer(b);
		} else {
			putBytesDirect(b);
		}
	}

	public void putBytes(byte[] b) throws IOException {
		if(b.length <= buf.length >> 1) {
			putBytesBuffer(b);
		} else {
			putBytesDirect(b);
		}
	}

	private void putBytesBuffer(byte[] b, int off, int len) throws IOException {
		if(buf.length < pos + len) {
			flushAlways();
		}
		System.arraycopy(b, off, buf, pos, len);
		pos += len;
	}

	public void putBytesDirect(byte[] b, int off, int len) throws IOException {
		if(pos != 0) {
			flushAlways();
		}
		out.write(b, off, len);		
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(len <= buf.length >> 1) {
			putBytesBuffer(b, off, len);
		} else {
			putBytesDirect(b, off, len);
		}
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
	
	public abstract void putShort(short v) throws IOException;
	public abstract void putChar(char v) throws IOException;
	public abstract void putInt(int v) throws IOException;
	public abstract void putFloat(float f) throws IOException;
	public abstract void putLong(long v) throws IOException;
	public abstract void putDouble(double d) throws IOException;
	
	public void putBooleans(boolean[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putBoolean(vs[i]);
		}
	}
	
	public void putShorts(short[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putShort(vs[i]);
		}
	}
	
	public void putShorts(short[] vs, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			putShort(vs[i]);
		}
	}
	
	public void putChars(char[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putChar(vs[i]);
		}
	}
	
	public void putInts(int[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putInt(vs[i]);
		}
	}
	
	public void putIntsOverflowZero(long[] vs, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			long v = vs[i];
			putInt(v > Integer.MAX_VALUE ? 0 : (int) v);
		}
	}
	
	public void putFloats(float[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putFloat(vs[i]);
		}
	}
	
	public void putLongs(long[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putLong(vs[i]);
		}
	}
	
	public void putDoubles(double[] vs) throws IOException {
		int len = vs.length;
		for (int i = 0; i < len; i++) {
			putDouble(vs[i]);
		}
	}
}
