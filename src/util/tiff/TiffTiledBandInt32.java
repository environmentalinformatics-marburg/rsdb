package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

public abstract class TiffTiledBandInt32 extends TiffTiledBand {	

	public TiffTiledBandInt32(int width, int height, int tileWidth, int tileHeight, String description) {
		super(width, height, tileWidth, tileHeight, description);
	}

	protected abstract Iterator<int[][]> getTiles();

	@Override
	public short getBitsPerSample() {
		return 32;
	}

	@Override
	public short getSampleFormat() {
		return 2; // two’s complement signed integer data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		Iterator<int[][]> it = getTiles();
		while(it.hasNext()) {
			int[][] data = it.next();
			TiffBandInt32.writeData(out, data, tileWidth, tileHeight);
		}
	}
}
