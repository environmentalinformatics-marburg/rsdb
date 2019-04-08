package util.raster;

public class CrsTransformer {
	
	public double[] toCrs(int x, int y) {
		return new double[]{x, y};
	}
	
	public int[] toPixel(double x, double y) {
		return new int[]{(int) x, (int) y};
	}

}
