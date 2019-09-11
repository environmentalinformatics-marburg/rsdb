package util.png;

import java.io.IOException;

class IEND_chunk extends PNGchunk {

	public static final IEND_chunk DEFAULT = new IEND_chunk();

	public IEND_chunk() {
		super(0, "IEND");
	}

	@Override
	protected void chunkWrite(CRC32DataOutput out) throws IOException {			
	}		
}