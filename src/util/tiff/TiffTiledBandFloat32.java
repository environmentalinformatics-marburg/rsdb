package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;


import org.tinylog.Logger;

public abstract class TiffTiledBandFloat32 extends TiffTiledBand {	
	

	public TiffTiledBandFloat32(int width, int height, int tileWidth, int tileHeight, String description) {
		super(width, height, tileWidth, tileHeight, description);
	}

	protected abstract Iterator<float[][]> getTiles();

	@Override
	public short getBitsPerSample() {
		return 32;
	}

	@Override
	public short getSampleFormat() {
		return 3; // floating point data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		Iterator<float[][]> it = getTiles();
		while(it.hasNext()) {
			float[][] data = it.next();
			TiffBandFloat32.writeData(out, data, tileWidth, tileHeight);
		}
	}

}
