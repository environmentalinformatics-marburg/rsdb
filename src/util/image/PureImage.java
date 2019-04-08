package util.image;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface to output image data to BufferedImage or to OutputStream as jpeg.
 * @author woellauer
 *
 */
public interface PureImage {

	BufferedImage getBufferedImage();
	void writeJpg(OutputStream out, float quality) throws IOException;

	/**
	 * write png with compression and fast filtering (FILTER_ADAPTIVE_FAST).
	 * @param out
	 * @param level no compression 0,  few compression 1 to best compression 9
	 */
	void writePng(OutputStream out, int level) throws IOException;

	void writePngCompressed(OutputStream out) throws IOException;

	default void writePngCompressed(String filename) throws IOException {
		try(FileOutputStream out = new FileOutputStream(filename)) {
			writePngCompressed(out);
		}
	}

}
