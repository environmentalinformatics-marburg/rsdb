package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class TiffBandFloat32 extends TiffBand {

	public TiffBandFloat32(int width, int height, String description) {
		super(width, height, description);
	}
	
	protected abstract float[][] getData();

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
		writeData(out, getData(), width, height);
	}

	public static void writeData(DataOutput out, float[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] target = null;
		for(int y = (height - 1); y >= 0; y--) {
			float[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.floatToByteArrayBigEndian(row, target);
			out.write(target);
		}		
	}
}
