package util.raster;

import java.io.DataOutput;
import java.io.IOException;

public class ShortSubRaster extends Raster {
	public final short[][] data;

	public ShortSubRaster(short[][] data, int xmin, int xmax, int ymin, int ymax, int xoffset, int yoffset) {
		super(xmin, xmax, ymin, ymax, xoffset, yoffset);
		this.data = data;		
	}
	
	public static ShortSubRaster of(ShortSubRaster source) {
		short[][] src = source.data;
		int xlen = src[0].length;
		int ylen = src.length;
		short[][] dst = new short[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(src[y], 0, dst[y], 0, xlen);
		}
		return new ShortSubRaster(dst, source.xmin, source.xmax, source.ymin, source.ymax, source.xoffset, source.yoffset);
	}
	
	public void writeDataBE(DataOutput out) throws IOException {
		RasterWriterBE.write(data, xmin, ymin, xmax, ymax, out);	
	}
	
	public void writeDataFlipRowsBE(DataOutput out) throws IOException {
		RasterWriterBE.writeFlipRows(data, xmin, ymin, xmax, ymax, out);	
	}
}
