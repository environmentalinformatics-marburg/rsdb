package util.rdat;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class RdatBandFloat32 extends RdatBand {
	
	public RdatBandFloat32(int width, int height, RdatList meta) {
		super(width, height, meta);
	}
	
	@Override
	public int getType() {
		return Rdat.TYPE_FLOAT32;		
	}
	
	@Override
	public int getBytesPerSample() {
		return Rdat.TYPE_FLOAT32_SIZE;
	}	

	protected abstract float[][] getData();

	@Override
	public void writeData(DataOutput out) throws IOException {
		byte[] target = null;
		float[][] data = getData();
		int w = width;
		int h = height;
		if(data.length != h) {
			throw new RuntimeException();
		}
		for(int y = (height - 1); y >= 0; y--) {
			float[] row = data[y];
			if(row.length != w) {
				throw new RuntimeException();
			}
			target = Serialisation.floatToByteArrayBigEndian(row, target);
			out.write(target);
		}		
	}
}
