package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.Serialisation;

public abstract class TiffBandFloat64Slice extends TiffBandFloat64 {
	
	public final int xoff;
	public final int yoff;

	public TiffBandFloat64Slice(int xoff, int yoff, int xlen, int ylen) {
		super(xlen, ylen);
		this.xoff = xoff;
		this.yoff = yoff;
	}	

	@Override
	public void writeData(DataOutput out) throws IOException {
		byte[] target = null;
		double[][] data = getData();
		int xlen = width;
		int ylen = height;
		if(data.length - yoff < ylen) {
			throw new RuntimeException();
		}
		for(int y = (yoff + ylen - 1); y >= yoff; y--) {
			double[] row = data[y];
			if(row.length - xoff < xlen) {
				throw new RuntimeException();
			}
			target = Serialisation.doubleToByteArrayBigEndian(row, xoff, xlen, target);
			out.write(target);
		}
	}	
}
