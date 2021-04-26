package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TiffTiledBandInt16 extends TiffTiledBand {	
	private static final Logger log = LogManager.getLogger();

	public TiffTiledBandInt16(int width, int height, int tileWidth, int tileHeight, String description) {
		super(width, height, tileWidth, tileHeight, description);
	}

	protected abstract Iterator<short[][]> getTiles();

	@Override
	public short getBitsPerSample() {
		return 16;
	}

	@Override
	public short getSampleFormat() {
		return 2; // two’s complement signed integer data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		Iterator<short[][]> it = getTiles();
		while(it.hasNext()) {
			short[][] data = it.next();
			TiffBandInt16.writeData(out, data, tileWidth, tileHeight);
		}
	}

}
