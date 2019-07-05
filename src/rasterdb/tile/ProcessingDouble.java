package rasterdb.tile;

public class ProcessingDouble {
	
	public static double[][] createEmpty(int width, int height) {
		double[][] data = new double[height][width];
		for (int y = 0; y < height; y++) {
			double[] row = data[y];
			for (int x = 0; x < width; x++) {
				row[x] = Double.NaN;
			}
		}
		return data;
	}
	
	public static double[][] copy(double[][] data, int width, int height) {
		double[][] d = new double[height][width];
		for (int i = 0; i < height; i++) {
		    System.arraycopy(data[i], 0, d[i], 0, width);
		}
		return d;
	}
	
	public static double[][] minus(double[][] a, double[][] b, int width, int height) {
		double[][] c = new double[height][width];
		for (int y = 0; y < height; y++) {
			double[] rowa = a[y];
			double[] rowb = b[y];
			double[] rowc = c[y];
			for (int x = 0; x < width; x++) {
				rowc[x] = rowa[x] - rowb[x];
			}
		}
		return c;
	}
	
	/**
	 * Write pixle of add to dst if add is finite
	 * @param dst
	 * @param add
	 */
	public static void merge(double[][] dst, double[][] add) {
		int ylen = dst.length;
		int xlen = dst[0].length;
		for (int y = 0; y < ylen; y++) {
			double[] dstRow = dst[y];
			double[] addRow = add[y];
			for (int x = 0; x < xlen; x++) {
				double v = addRow[x];
				if(Double.isFinite(v)) {
					dstRow[x] = v;
				}
			}
		}
	}
	
}
