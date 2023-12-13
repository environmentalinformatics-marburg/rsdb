package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

/**
 * Not usable, because GDAL interprets tiff int8 as uint 8.
 *
 */
public abstract class TiffTiledBandInt8 extends TiffTiledBand {	

	public TiffTiledBandInt8(int width, int height, int tileWidth, int tileHeight, String description) {
		super(width, height, tileWidth, tileHeight, description);
	}

	protected abstract Iterator<byte[][]> getTiles();

	@Override
	public short getBitsPerSample() {
		return 8;
	}

	@Override
	public short getSampleFormat() {
		return 2; // two’s complement signed integer data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		Iterator<byte[][]> it = getTiles();
		while(it.hasNext()) {
			byte[][] data = it.next();
			TiffBandInt8.writeData(out, data, tileWidth, tileHeight);
		}
	}
}
