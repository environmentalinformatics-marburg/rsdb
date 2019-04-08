package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class TiffBandInt16 extends TiffBand {

	public TiffBandInt16(int width, int height) {
		super(width, height);
	}

	protected abstract short[][] getData();

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
		writeData(out, getData(), width, height);
	}

	public static void writeData(DataOutput out, short[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] target = null;
		for(int y = (height - 1); y >= 0; y--) {
			short[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.shortToByteArrayBigEndian(row, target);
			out.write(target);
		}
	}
}
