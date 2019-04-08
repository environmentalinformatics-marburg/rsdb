package util.raster;

import java.io.DataOutput;
import java.io.IOException;

public class DoubleSubRaster extends Raster {
	public final double[][] data;

	public DoubleSubRaster(double[][] data, int xmin, int xmax, int ymin, int ymax, int xoffset, int yoffset) {
		super(xmin, xmax, ymin, ymax, xoffset, yoffset);
		this.data = data;		
	}
	
	public void writeDataBE(DataOutput out) throws IOException {
		RasterWriterBE.write(data, xmin, ymin, xmax, ymax, out);	
	}
	
	public void writeDataFlipRowsBE(DataOutput out) throws IOException {
		RasterWriterBE.writeFlipRows(data, xmin, ymin, xmax, ymax, out);	
	}
	
	public static DoubleSubRaster of(DoubleSubRaster source) {
		double[][] src = source.data;
		int xlen = src[0].length;
		int ylen = src.length;
		double[][] dst = new double[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(src[y], 0, dst[y], 0, xlen);
		}
		return new DoubleSubRaster(dst, source.xmin, source.xmax, source.ymin, source.ymax, source.xoffset, source.yoffset);
	}
	
	public static DoubleSubRaster of(ShortSubRaster source) {
		short[][] src = source.data;
		int xlen = src[0].length;
		int ylen = src.length;
		double[][] dst = new double[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			short[] srcRow = src[y];
			double[] dstRow = dst[y];
			for (int x = 0; x < xlen; x++) {
				dstRow[x] = srcRow[x];
			}
		}
		return new DoubleSubRaster(dst, source.xmin, source.xmax, source.ymin, source.ymax, source.xoffset, source.yoffset);
	}
}
