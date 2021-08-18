package util.rdat;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class RdatBandBool8 extends RdatBand {
	
	public RdatBandBool8(int width, int height, RdatList meta) {
		super(width, height, meta);
	}
	
	@Override
	public int getType() {
		return Rdat.TYPE_LOGICAL;		
	}
	
	@Override
	public int getBytesPerSample() {
		return Rdat.TYPE_LOGICAL_SIZE;
	}	

	protected abstract boolean[][] getData();

	@Override
	public void writeData(DataOutput out) throws IOException {
		byte[] target = null;
		boolean[][] data = getData();
		int w = width;
		int h = height;
		if(data.length != h) {
			throw new RuntimeException();
		}
		for(int y = (height - 1); y >= 0; y--) {
			boolean[] row = data[y];
			if(row.length != w) {
				throw new RuntimeException();
			}
			target = Serialisation.booleanToByteArray(row, target);
			out.write(target);
		}		
	}
}
