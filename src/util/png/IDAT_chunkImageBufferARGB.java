package util.png;

import java.io.IOException;

import util.image.ImageBufferARGB;

public class IDAT_chunkImageBufferARGB extends IDAT_chunk {

	private final ImageBufferARGB image;

	public IDAT_chunkImageBufferARGB(ImageBufferARGB image) {
		super(image.width, image.height);
		this.image = image;
	}

	@Override
	protected void writeData(DeflateWriter deflateWriter) throws IOException {
		int[] src = image.data;
		byte[] row = new byte[1 + 4 * width];
		int srcPos = 0;
		int w = width;
		int h = height;
		for (int i = 0; i < h; i++) {
			int dstPos = 1;
			for(int j = 0; j < w; j++) {
				int v = src[srcPos++];
				row[dstPos + 3] = (byte) (v >> 24); // alpha
				row[dstPos] = (byte) (v >> 16); // blue
				row[dstPos + 1] = (byte) (v >> 8); // green
				row[dstPos + 2] = (byte) v; // red
				dstPos += 4;
			}
			deflateWriter.write(row);
		}
		/*int[] src = image.data;
		byte[] dst = new byte[(1 + 4 * width) * height];
		int srcPos = 0;
		int w = width;
		int h = height;
		int dstPos = 0;
		for (int i = 0; i < h; i++) {
			dstPos++;
			for(int j = 0; j < w; j++) {
				int v = src[srcPos++];
				dst[dstPos + 3] = (byte) (v >> 24); // alpha
				dst[dstPos] = (byte) (v >> 16); // blue
				dst[dstPos + 1] = (byte) (v >> 8); // green
				dst[dstPos + 2] = (byte) v; // red
				dstPos += 4;
			}
		}
		deflateWriter.write(dst);*/
	}
}
