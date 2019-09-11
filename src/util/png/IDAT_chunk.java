package util.png;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

class IDAT_chunk extends PNGchunk {
	public final int width;
	public final int height;
	private final int dataLen;
	

	public IDAT_chunk(int width, int height) {
		this(width, height, (1 + 4*width) * height);
	}
	
	private IDAT_chunk(int width, int height, int dataLen) {
		super(DeflateWriter.getDeflateLen(dataLen), "IDAT");
		this.width = width;
		this.height = height;
		this.dataLen = dataLen;
	}

	@Override
	protected void chunkWrite(CRC32DataOutput out) throws IOException {
		DeflateWriter deflateWriter = new DeflateWriter(out, dataLen);
		writeData(deflateWriter);
	}
	
	protected void writeData(DeflateWriter deflateWriter) throws IOException {
		byte[] row = new byte[1 + 4*width];
		for (int i = 0; i < height; i++) {
			
			row[0] = 0;
			for(int j = 1; j < row.length; j++) {
				//row[j] = (byte) 0x77;
				row[j] = (byte) ThreadLocalRandom.current().nextInt();
			}				
			deflateWriter.write(row);
		}
	}
	
	
	
				
}