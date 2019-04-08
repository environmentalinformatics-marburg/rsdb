package util;

import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayOutStream extends OutputStream {

	private byte[] buf;
	private int pos;
	private ByteArrayOut out;

	public ByteArrayOutStream(ByteArrayOut out) {
		this.buf = out.buf;
		this.pos = out.pos;
		this.out = out;
	}

	private void ensureCapacity(int size) {
		if(buf.length<size) {
			grow(size);
		}		
	}

	private void grow(int minSize) {
		out.grow(minSize);
		relaod();
	}


	@Override
	public void write(int v) throws IOException {
		int p = pos;
		ensureCapacity(p+1);
		buf[p] = (byte) v;
		pos = p+1;
	}

	@Override
	public void write(byte[] vs, int offset, int len) throws IOException {
		int p = pos;
		ensureCapacity(p+len);
		System.arraycopy(vs, offset, buf, p, len);
		pos = p+len;
	}

	@Override
	public void flush() throws IOException {
		out.buf = this.buf;
		out.pos = this.pos;
	}

	@Override
	public void close() throws IOException {
		flush();
		this.buf = null;
		this.pos = 0;
		this.out = null;
	}

	public void relaod() {
		buf = out.buf;
		pos = out.pos;
	}
}
