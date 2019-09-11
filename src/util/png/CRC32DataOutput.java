package util.png;

import java.io.DataOutput;
import java.io.IOException;
import java.util.zip.CRC32;

public class CRC32DataOutput {
	private final DataOutput out;
	private final CRC32 crc32 = new CRC32();

	public CRC32DataOutput(DataOutput out) {
		this.out = out;
	}

	public int getCRC32() {
		return (int) crc32.getValue();
	}

	public void writeByte(byte b) throws IOException {
		out.write(b);
		crc32.update(b);
	}		

	public void writeInt(int v) throws IOException {
		out.writeInt(v);			
		crc32.update(v >>> 24);
		crc32.update(v >>> 16);
		crc32.update(v >>>  8);
		crc32.update(v >>>  0);
	}
	
	public void writeShortLittleEndian(short v) throws IOException {
		int b0 = v >>>  0;
		int b1 = v >>>  8;
		out.writeByte(b0);
		out.writeByte(b1);
		crc32.update(b0);
		crc32.update(b1);
	}

	public void write(byte[] b) throws IOException {
		out.write(b);
		crc32.update(b);
	}
	
	void write(byte b[], int off, int len) throws IOException {
		out.write(b, off, len);
		crc32.update(b, off, len);
	}		
}