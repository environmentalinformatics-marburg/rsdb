package util.png;

import java.io.IOException;
import java.util.zip.Adler32;

public class DeflateWriter {
	private final CRC32DataOutput out;
	private final Adler32 adler32 = new Adler32();
	private final int len;
	private int pos = 0;
	private static final int MAX_BLOCK = 65535;

	public DeflateWriter(CRC32DataOutput out, int len) throws IOException {
		this.len = len;
		this.out = out;
		out.writeByte((byte) 0x78);
		out.writeByte((byte) 0x01);
	}

	public void write(byte[] b) throws IOException {
		int bpos = 0;
		int blen = b.length;
		while(bpos < blen) {
			if(pos % MAX_BLOCK == 0) {
				if(pos / MAX_BLOCK < len / MAX_BLOCK) {
					out.writeByte((byte) 0);
					out.writeShortLittleEndian((short) MAX_BLOCK);
					out.writeShortLittleEndian((short) ~MAX_BLOCK);
				} else {
					out.writeByte((byte) 1);
					out.writeShortLittleEndian((short) (len - pos));
					out.writeShortLittleEndian((short) ~(len - pos));					
				}						
			}
			int rem = MAX_BLOCK - (pos % MAX_BLOCK);
			if(bpos + rem > blen) {
				rem = blen - bpos;
			}
			out.write(b, bpos, rem);
			adler32.update(b, bpos, rem);
			pos +=rem;
			bpos += rem;
		}
		if(pos == len) {
			out.writeInt((int) adler32.getValue());
		}
	}

	public static int getDeflateLen(int len) {
		int mod = len % MAX_BLOCK;
		int div = len / MAX_BLOCK;
		int r = 2 + (mod == 0 ? div : div + 1) * 5 + len + 4;
		return r;
	}
}