package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public abstract class TiffBandUint8 extends TiffBand {

	public TiffBandUint8(int width, int height, String description) {
		super(width, height, description);
	}
	
	protected abstract byte[][] getData();

	@Override
	public short getBitsPerSample() {
		return 8;
	}

	@Override
	public short getSampleFormat() {
		return 1; // unsigned integer data
	}
	
	@Override
	public void writeData(DataOutput out) throws IOException {
		writeData(out, getData(), width, height);
	}	

	private void writeData(DataOutput out, byte[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		for(int y = (height - 1); y >= 0; y--) {
			byte[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			out.write(row);
		}		
	}
}
