package util.raster;

public class DirectFlatMatrix {
	
	public final double[] data;
	public final int stride;
	
	public DirectFlatMatrix(int width, int height) {
		this(new double[height * width], width);
	}
	
	public DirectFlatMatrix(double[] data, int width) {
		this.data = data;
		this.stride = width;
	}
	
	public int idx(int x, int y) {
		return idx(stride, x, y);
	}
	
	public static int idx(int stride, int x, int y) {
		return stride * y + x;
	}
	
	public void set(int x, int y, double v) {
		data[idx(x, y)] = v;
	}

	public double get(int x, int y) {
		return data[idx(x, y)];
	}
}
