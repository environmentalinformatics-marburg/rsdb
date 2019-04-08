package util.raster;

public class Matrix {
	
	public final double[][] data;
	public final int xoff;
	public final int yoff;
	public final int xlen;
	public final int ylen;
	
	public Matrix(double[][] data, int xoff, int yoff, int xlen, int ylen) {
		this.data = data;
		this.xoff = xoff;
		this.yoff = yoff;
		this.xlen = xlen;
		this.ylen = ylen;
	}
	
	public void set(int x, int y, double v) {
		data[y + yoff][x + xoff] = v;
	}

	public double get(int x, int y) {
		return data[y + yoff][x + xoff];
	}
}
