package util.raster;

import java.io.DataOutput;
import java.io.IOException;

public class DoubleRaster extends DoubleSubRaster {
	
	public DoubleRaster(double[][] data, int xoffset, int yoffset) {
		super(data, 0, data[0].length, 0, data.length, xoffset, yoffset);
	}
	
	public DoubleRaster(int width, int height, int xoffset, int yoffset) {
		super(new double[height][width], 0, width, 0, height, xoffset, yoffset);
	}	
	
	@Override
	public void writeDataBE(DataOutput out) throws IOException {
		RasterWriterBE.write(data, out);		
	}
	
	@Override
	public void writeDataFlipRowsBE(DataOutput out) throws IOException {
		RasterWriterBE.writeFlipRows(data, out);		
	}
	
	public static DoubleRaster of(DoubleRaster source) {
		double[][] src = source.data;
		int xlen = source.width;
		int ylen = source.height;
		double[][] dst = new double[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(src[y], 0, dst[y], 0, xlen);
		}
		return new DoubleRaster(dst, source.xoffset, source.yoffset);
	}
	
	public static DoubleRaster of(DoubleSubRaster source) {
		double[][] src = source.data;
		int xlen = source.width;
		int ylen = source.height;
		int xoffset = source.xmin;
		int yoffset = source.ymin;
		double[][] dst = new double[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(src[y], xoffset, dst[y], 0, xlen);
		}
		return new DoubleRaster(dst, source.xoffset+xoffset, source.yoffset+yoffset);
	}
	
	public static DoubleRaster of(ShortRaster source) {
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
		return new DoubleRaster(dst, source.xoffset, source.yoffset);
	}
	
	public static DoubleRaster of(ShortSubRaster source) {
		short[][] src = source.data;
		int xlen = source.width;
		int ylen = source.height;
		int xoffset = source.xmin;
		int yoffset = source.ymin;
		double[][] dst = new double[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			short[] srcRow = src[y+yoffset];
			double[] dstRow = dst[y];
			for (int x = 0; x < xlen; x++) {
				dstRow[x] = srcRow[x+xoffset];
			}
		}
		return new DoubleRaster(dst, source.xoffset+xoffset, source.yoffset+yoffset);
	}
}
