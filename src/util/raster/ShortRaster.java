package util.raster;

import java.io.DataOutput;
import java.io.IOException;

public class ShortRaster extends ShortSubRaster {
	
	public ShortRaster(short[][] data, int xoffset, int yoffset) {
		super(data, 0, data[0].length, 0, data.length, xoffset, yoffset);
	}
	
	public ShortRaster(int width, int height, int xoffset, int yoffset) {
		super(new short[height][width], 0, width, 0, height, xoffset, yoffset);
	}
	
	public static ShortRaster of(ShortRaster source) {
		short[][] src = source.data;
		int xlen = source.width;
		int ylen = source.height;
		short[][] dst = new short[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(src[y], 0, dst[y], 0, xlen);
		}
		return new ShortRaster(dst, source.xoffset, source.yoffset);
	}
	
	public static ShortRaster of(ShortSubRaster source) {
		short[][] src = source.data;
		int xlen = source.width;
		int ylen = source.height;
		int xoffset = source.xmin;
		int yoffset = source.ymin;
		short[][] dst = new short[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(src[y], xoffset, dst[y], 0, xlen);
		}
		return new ShortRaster(dst, source.xoffset+xoffset, source.yoffset+yoffset);
	}	
	
	@Override
	public void writeDataBE(DataOutput out) throws IOException {
		RasterWriterBE.write(data, out);		
	}
	
	@Override
	public void writeDataFlipRowsBE(DataOutput out) throws IOException {
		RasterWriterBE.writeFlipRows(data, out);		
	}
}
