package util.rdat;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class RdatBandFloat64 extends RdatBand {
	
	public RdatBandFloat64(int width, int height, RdatList meta) {
		super(width, height, meta);
	}
	
	@Override
	public int getType() {
		return Rdat.TYPE_FLOAT64;		
	}
	
	@Override
	public int getBytesPerSample() {
		return Rdat.TYPE_FLOAT64_SIZE;
	}	

	protected abstract double[][] getData();

	@Override
	public void writeData(DataOutput out) throws IOException {
		byte[] target = null;
		double[][] data = getData();
		int w = width;
		int h = height;
		if(data.length != h) {
			throw new RuntimeException();
		}
		for(int y = (height - 1); y >= 0; y--) {
			double[] row = data[y];
			if(row.length != w) {
				throw new RuntimeException();
			}
			target = Serialisation.doubleToByteArrayBigEndian(row, target);
			out.write(target);
		}		
	}
}
