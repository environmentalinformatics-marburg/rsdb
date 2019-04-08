package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public class DataOutputCounter implements DataOutput {
	
	private long pos = 0;

	@Override
	public void write(int b) throws IOException {
		pos += 4;		
	}

	@Override
	public void write(byte[] b) throws IOException {
		pos += b.length;		
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		pos += len;		
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		pos++;		
	}

	@Override
	public void writeByte(int v) throws IOException {
		pos++;
		
	}

	@Override
	public void writeShort(int v) throws IOException {
		pos += 2;
		
	}

	@Override
	public void writeChar(int v) throws IOException {
		pos += 2;
		
	}

	@Override
	public void writeInt(int v) throws IOException {
		pos += 4;
		
	}

	@Override
	public void writeLong(long v) throws IOException {
		pos += 8;
		
	}

	@Override
	public void writeFloat(float v) throws IOException {
		pos += 4;
		
	}

	@Override
	public void writeDouble(double v) throws IOException {
		pos += 8;		
	}

	@Override
	public void writeBytes(String s) throws IOException {
		pos += s.length();		
	}

	@Override
	public void writeChars(String s) throws IOException {
		pos += 2 * s.length();		
	}

	@Override
	public void writeUTF(String s) throws IOException {
		throw new RuntimeException("not implemented");		
	}
	
	public long getPos() {
		return pos;
	}

}
