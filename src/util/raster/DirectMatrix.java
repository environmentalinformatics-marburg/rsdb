package util.raster;

public class DirectMatrix {
	
	public final double[] data;
	public final int stride;
	public final int offset;
	
	public final int xlen;
	public final int ylen;
	
	public DirectMatrix(int width, int height) {
		this(new double[height * width], width, height);
	}
	
	public DirectMatrix(double[] data, int width, int height) {
		this(data, width, 0, 0, width, height);
	}
	
	public DirectMatrix(double[] data, int width, int xoff, int yoff, int xlen, int ylen) {
		this.data = data;
		this.xlen = xlen;
		this.ylen = ylen;
		this.stride = width;
		this.offset = yoff * width + xoff;
	}
	
	public int idx(int x, int y) {
		return idx(stride, offset, x, y);
	}
	
	public static int idx(int stride, int offset, int x, int y) {
		return stride * y + x + offset;
	}
	
	public void set(int x, int y, double v) {
		data[idx(x, y)] = v;
	}

	public double get(int x, int y) {
		return data[idx(x, y)];
	}
	
	public int xLowerBorder() {
		return offset % stride;
	}
	
	public int yLowerBorder() {
		return offset / stride;
	}
	
	public int xUpperBorder() {
		return stride - xlen - xLowerBorder();
	}
	
	public int yUpperBorder() {
		return (data.length - stride * ylen - offset) / stride;
	}
	
	public boolean noBorders() {
		return offset == 0 && stride == xlen && data.length == stride * ylen;
	}
	
	public DirectMatrix toNoBorders() {
		return new DirectMatrix(data, stride, data.length / stride);
	}
}
