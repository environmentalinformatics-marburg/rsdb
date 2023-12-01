package util;

public class Filler {
	
	public static boolean fillG(double[][] src, double[][] dst, int width, int height, int fx, int fy, int radius) {
		int xmin = fx - radius;
		int ymin = fy - radius;
		int xmax = fx + radius;
		int ymax = fy + radius;
		int cxmin = xmin < 0 ? 0 : xmin;
		int cymin = ymin < 0 ? 0 : ymin;
		int cxmax = xmax >= width ? width - 1 : xmax;
		int cymax = ymax >= height ? height - 1 : ymax;

		double cnt = 0;
		double sum = 0;

		if(ymin >= 0) {
			double[] row = src[ymin];
			for (int ix = cxmin; ix <= cxmax; ix++) {
				double v = row[ix];
				if(Double.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(ymax < height) {
			double[] row = src[ymax];
			for (int ix = cxmin; ix <= cxmax; ix++) {
				double v = row[ix];
				if(Double.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(xmin >= 0) {
			for (int iy = cymin + 1; iy < cymax; iy++) {
				double v = src[iy][xmin];
				if(Double.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(xmax < width) {
			for (int iy = cymin + 1; iy < cymax; iy++) {
				double v = src[iy][xmax];
				if(Double.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(cnt > 0) {
			//Logger.info("fill "+fx+" "+fy+" "+cnt);
			dst[fy][fx] = sum / cnt;
			return true;
		}
		return false;
	}
	
	public static boolean fillG(float[][] src, float[][] dst, int width, int height, int fx, int fy, int radius) {
		int xmin = fx - radius;
		int ymin = fy - radius;
		int xmax = fx + radius;
		int ymax = fy + radius;
		int cxmin = xmin < 0 ? 0 : xmin;
		int cymin = ymin < 0 ? 0 : ymin;
		int cxmax = xmax >= width ? width - 1 : xmax;
		int cymax = ymax >= height ? height - 1 : ymax;

		float cnt = 0;
		float sum = 0;

		if(ymin >= 0) {
			float[] row = src[ymin];
			for (int ix = cxmin; ix <= cxmax; ix++) {
				float v = row[ix];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(ymax < height) {
			float[] row = src[ymax];
			for (int ix = cxmin; ix <= cxmax; ix++) {
				float v = row[ix];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(xmin >= 0) {
			for (int iy = cymin + 1; iy < cymax; iy++) {
				float v = src[iy][xmin];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(xmax < width) {
			for (int iy = cymin + 1; iy < cymax; iy++) {
				float v = src[iy][xmax];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(cnt > 0) {
			//Logger.info("fill "+fx+" "+fy+" "+cnt);
			dst[fy][fx] = sum / cnt;
			return true;
		}
		return false;
	}
}
