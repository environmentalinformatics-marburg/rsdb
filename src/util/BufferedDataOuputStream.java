package util;

import java.io.IOException;
import java.io.OutputStream;

public class BufferedDataOuputStream extends BufferedDataOuputStreamBasic {

	public BufferedDataOuputStream(OutputStream out) {
		super(out);
	}
	
	public BufferedDataOuputStream(OutputStream out, int size) {
		super(out, size);
	}

	public void putShort(short v) throws IOException {
		if(buf.length < pos + 2) {
			flushAlways();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeShort(int v) throws IOException {
		if(buf.length < pos + 2) {
			flushAlways();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;		
	}

	public void putChar(char v) throws IOException {
		if(buf.length < pos + 2) {
			flushAlways();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;
	}

	@Override
	public void writeChar(int v) throws IOException {
		if(buf.length < pos + 2) {
			flushAlways();
		}
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;			
	}

	public void putInt(int v) throws IOException {
		if(buf.length < pos + 4) {
			flushAlways();
		}
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;	
	}

	@Override
	public void writeInt(int v) throws IOException {
		if(buf.length < pos + 4) {
			flushAlways();
		}
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;	
	}

	public void putFloat(float f) throws IOException {
		if(buf.length < pos + 4) {
			flushAlways();
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
			flushAlways();
		}
		int v = Float.floatToRawIntBits(f);
		buf[pos++] = (byte) (v >>> 24);
		buf[pos++] = (byte) (v >>> 16);
		buf[pos++] = (byte) (v >>> 8);
		buf[pos++] = (byte) v;			
	}

	public void putLong(long v) throws IOException {
		if(buf.length < pos + 8) {
			flushAlways();
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
			flushAlways();
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
			flushAlways();
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
			flushAlways();
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
}
