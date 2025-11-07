package util.png;

import java.io.DataOutput;
import java.io.IOException;

import util.image.ImageBufferARGB;

public class PngWriter {
	
	
	private static final long MAGIC = 0x89504E470D0A1A0Al;

	public static void write(DataOutput out, ImageBufferARGB image) throws IOException {
		out.writeLong(MAGIC);		
		IHDR_chunk.ofRGBA(image.width, image.height).write(out);
		new IDAT_chunkImageBufferARGB(image).write(out);
		IEND_chunk.DEFAULT.write(out);
	}

}
