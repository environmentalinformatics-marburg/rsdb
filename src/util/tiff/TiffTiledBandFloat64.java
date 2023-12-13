package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

public abstract class TiffTiledBandFloat64 extends TiffTiledBand {	
	

	public TiffTiledBandFloat64(int width, int height, int tileWidth, int tileHeight, String description) {
		super(width, height, tileWidth, tileHeight, description);
	}

	protected abstract Iterator<double[][]> getTiles();

	@Override
	public short getBitsPerSample() {
		return 64;
	}

	@Override
	public short getSampleFormat() {
		return 3; // floating point data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		Iterator<double[][]> it = getTiles();
		while(it.hasNext()) {
			double[][] data = it.next();
			TiffBandFloat64.writeData(out, data, tileWidth, tileHeight);
		}
	}
}
