package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class TiffBandUint16 extends TiffBand {

	public TiffBandUint16(int width, int height, String description) {
		super(width, height, description);
	}

	protected abstract char[][] getData();

	@Override
	public short getBitsPerSample() {
		return 16;
	}

	@Override
	public short getSampleFormat() {
		return 1; // unsigned integer data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		writeData(out, getData(), width, height);
	}

	public static void writeData(DataOutput out, char[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] target = null;
		for(int y = (height - 1); y >= 0; y--) {
			char[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.charToByteArrayBigEndian(row, target);
			out.write(target);
		}
	}	
}
