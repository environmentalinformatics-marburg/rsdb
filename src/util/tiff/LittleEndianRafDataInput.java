package util.tiff;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class LittleEndianRafDataInput implements DataInput {

	public final RandomAccessFile raf;

	public LittleEndianRafDataInput(RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		raf.readFully(b);		
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		raf.readFully(b, off, len);		
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return raf.skipBytes(n);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return raf.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return raf.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return raf.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		int ch1 = raf.read();
		int ch2 = raf.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (short)((ch2 << 8) + (ch1 << 0));
	}

	@Override
	public int readUnsignedShort() throws IOException {
		int ch1 = raf.read();
		int ch2 = raf.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (ch2 << 8) + (ch1 << 0);
	}

	@Override
	public char readChar() throws IOException {
		int ch1 = raf.read();
		int ch2 = raf.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
        return (char)((ch2 << 8) + (ch1 << 0));
	}

	@Override
	public int readInt() throws IOException {
		int ch1 = raf.read();
        int ch2 = raf.read();
        int ch3 = raf.read();
        int ch4 = raf.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
	}

	@Override
	public long readLong() throws IOException {
		return ((readInt() & 0xFFFFFFFFL + (long)(readInt()) << 32));
	}

	@Override
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	@Override
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	@Override
	public String readLine() throws IOException {
		return raf.readLine();
	}

	@Override
	public String readUTF() throws IOException {
		return raf.readUTF();
	}
}
