package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class TiffBandInt32 extends TiffBand {

	public TiffBandInt32(int width, int height, String description) {
		super(width, height, description);
	}

	protected abstract int[][] getData();

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
		writeData(out, getData(), width, height);
	}

	public static void writeData(DataOutput out, int[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] target = null;
		for(int y = (height - 1); y >= 0; y--) {
			int[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.intToByteArrayBigEndian(row, target);
			out.write(target);
		}
	}
}
