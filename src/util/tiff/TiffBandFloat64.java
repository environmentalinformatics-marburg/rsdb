package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class TiffBandFloat64 extends TiffBand {

	public TiffBandFloat64(int width, int height) {
		super(width, height);
	}
	
	protected abstract double[][] getData();

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
		writeData(out, getData(), width, height);
	}	

	public static void writeData(DataOutput out, double[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] target = null;
		for(int y = (height - 1); y >= 0; y--) {
			double[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.doubleToByteArrayBigEndian(row, target);
			out.write(target);
		}		
	}
}
