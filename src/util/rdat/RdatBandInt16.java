package util.rdat;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class RdatBandInt16 extends RdatBand {
	
	public RdatBandInt16(int width, int height, RdatList meta) {
		super(width, height, meta);
	}
	
	@Override
	public int getType() {
		return Rdat.TYPE_INT16;		
	}
	
	@Override
	public int getBytesPerSample() {
		return Rdat.TYPE_INT16_SIZE;
	}	

	protected abstract short[][] getData();

	@Override
	public void writeData(DataOutput out) throws IOException {
		byte[] target = null;
		short[][] data = getData();
		int w = width;
		int h = height;
		if(data.length != h) {
			throw new RuntimeException();
		}
		for(int y = (height - 1); y >= 0; y--) {
			short[] row = data[y];
			if(row.length != w) {
				throw new RuntimeException();
			}
			target = Serialisation.shortToByteArrayBigEndian(row, target);
			out.write(target);
		}		
	}
}
