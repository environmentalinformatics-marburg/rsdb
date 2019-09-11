package util.png;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.image.ImageBufferARGB;

public class PngWriter {
	static final Logger log = LogManager.getLogger();
	
	private static final long MAGIC = 0x89504E470D0A1A0Al;

	public static void write(DataOutput out, ImageBufferARGB image) throws IOException {
		out.writeLong(MAGIC);		
		IHDR_chunk.ofRGBA(image.width, image.height).write(out);
		new IDAT_chunkImageBufferARGB(image).write(out);
		IEND_chunk.DEFAULT.write(out);
	}

}
