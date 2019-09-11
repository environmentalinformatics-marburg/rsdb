package util.png;

import java.io.DataOutput;
import java.io.IOException;

abstract class PNGchunk {
	public final int len;
	public final int type;
	public PNGchunk(int len, int type) {
		this.len = len;
		this.type = type;
	}
	public PNGchunk(int len, String typeFourCC) {
		this(len, fourCCtoInt(typeFourCC));
	}
	private static int fourCCtoInt(String typeFourCC) {
		char[] chars = typeFourCC.toCharArray();
		if(chars.length != 4) {
			throw new RuntimeException("wrong FourCC");
		}
		return ((byte)chars[0] << 24) | (((byte)chars[1]) << 16) | (((byte)chars[2]) << 8) | ((byte)chars[3]);
	}
	public final void write(DataOutput out) throws IOException {
		out.writeInt(len);
		CRC32DataOutput crc32DataOutput = new CRC32DataOutput(out);
		crc32DataOutput.writeInt(type);
		chunkWrite(crc32DataOutput);
		out.writeInt(crc32DataOutput.getCRC32());
	}

	protected abstract void chunkWrite(CRC32DataOutput out) throws IOException;
}