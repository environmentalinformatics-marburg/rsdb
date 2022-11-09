package util;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public class BufferedDataOuputStream extends OutputStream implements DataOutput {

	private static final int DEFAULT_BUFFER_SIZE = 65536;

	public byte[] buf;
	public int pos;

	private final OutputStream out;

	public BufferedDataOuputStream(OutputStream out) {
		this.buf = new byte[DEFAULT_BUFFER_SIZE];
		this.pos = 0;
		this.out = out;
	}

	public BufferedDataOuputStream(OutputStream out, int size) {
		this.buf = new byte[size];
		this.pos = 0;
		this.out = out;
	}

	public void flush() throws IOException {
		out.write(buf, 0, pos);
		pos = 0;
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
			flush();
		}
		buf[pos++] = v;
	}

	@Override
	public void writeByte(int b) throws IOException {
		if(buf.length == pos) {
			flush();
		}
		buf[pos++] = (byte) b;			
	}

	@Override
	public void write(int b) throws IOException {
		if(buf.length == pos) {
			flush();
		}
		buf[pos++] = (byte) b;	
	}

	public void putBoolean(boolean v) throws IOException {
		if(buf.length == pos) {
			flush();
		}
		buf[pos++] = (byte) (v ? 1 : 0);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		if(buf.length == pos) {
			flush();
		}
		buf[pos++] = (byte) (v ? 1 : 0);		
	}


	public void putShort(short v) throws IOException {
		if(buf.length < pos + 2) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeShort(int v) throws IOException {
		if(buf.length < pos + 2) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;		
	}

	public void putChar(char v) throws IOException {
		if(buf.length < pos + 2) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeChar(int v) throws IOException {
		if(buf.length < pos + 2) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;			
	}

	public void putInt(int v) throws IOException {
		if(buf.length < pos + 4) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;	
	}

	@Override
	public void writeInt(int v) throws IOException {
		if(buf.length < pos + 4) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;	
	}

	public void putFloat(float f) throws IOException {
		if(buf.length < pos + 4) {
			flush();
		}
		int v = Float.floatToRawIntBits(f);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeFloat(float f) throws IOException {
		if(buf.length < pos + 4) {
			flush();
		}
		int v = Float.floatToRawIntBits(f);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;			
	}

	public void putLong(long v) throws IOException {
		if(buf.length < pos + 8) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 56);
		buf[pos++] = (byte) (v >>> 48);
		buf[pos++] = (byte) (v >>> 40);
		buf[pos++] = (byte) (v >>> 32);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeLong(long v) throws IOException {
		if(buf.length < pos + 8) {
			flush();
		}
		buf[pos++] = (byte) (v >>> 56);
		buf[pos++] = (byte) (v >>> 48);
		buf[pos++] = (byte) (v >>> 40);
		buf[pos++] = (byte) (v >>> 32);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;		
	}

	public void putDouble(double d) throws IOException {
		if(buf.length < pos + 8) {
			flush();
		}
		long v = Double.doubleToRawLongBits(d);
		buf[pos++] = (byte) (v >>> 56);
		buf[pos++] = (byte) (v >>> 48);
		buf[pos++] = (byte) (v >>> 40);
		buf[pos++] = (byte) (v >>> 32);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeDouble(double d) throws IOException {
		if(buf.length < pos + 8) {
			flush();
		}
		long v = Double.doubleToRawLongBits(d);
		buf[pos++] = (byte) (v >>> 56);
		buf[pos++] = (byte) (v >>> 48);
		buf[pos++] = (byte) (v >>> 40);
		buf[pos++] = (byte) (v >>> 32);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;		
	}
	
	private void putBufferBytes(byte[] b) throws IOException {
		int len = b.length;
		if(buf.length < pos + len) {
			flush();
		}
		System.arraycopy(b, 0, buf, pos, len);
		pos += len;
	}
	
	private void putOutBytes(byte[] b) throws IOException {
		flush();
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		if(b.length <= buf.length >> 1) {
			putBufferBytes(b);
		} else {
			putOutBytes(b);
		}
	}
	
	private void putBufferBytes(byte[] b, int off, int len) throws IOException {
		if(buf.length < pos + len) {
			flush();
		}
		System.arraycopy(b, off, buf, pos, len);
		pos += len;
	}
	
	private void putOutBytes(byte[] b, int off, int len) throws IOException {
		flush();
		out.write(b, off, len);		
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(len <= buf.length >> 1) {
			putBufferBytes(b, off, len);
		} else {
			putOutBytes(b, off, len);
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
}
