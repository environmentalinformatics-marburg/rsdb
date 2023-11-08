package rasterdb.node;

import rasterdb.FrameProducer;
import util.frame.DoubleFrame;

public class ProcessorNode_gap_filling extends ProcessorNode {


	private final ProcessorNode node;
	private final int maxRadius;

	public ProcessorNode_gap_filling(ProcessorNode node, int maxRadius) {
		this.node = node;
		this.maxRadius = maxRadius;
	}

	@Override
	public DoubleFrame[] process(FrameProducer processor) {
		DoubleFrame[] frames = node.process(processor);
		for (int i = 0; i < frames.length; i++) {
			DoubleFrame frame = frames[i];
			if(frame.hasNA()) {
				frames[i] = fill(frame, maxRadius);
			}
		}
		return frames;
	}

	public static DoubleFrame fill(DoubleFrame frame, int maxRadius) {
		DoubleFrame resultFrame = frame.copy();
		double[][] src = frame.data;
		double[][] dst = resultFrame.data;
		int width = frame.width;
		int height = frame.height;
		fill(src, dst, width, height, maxRadius);
		return resultFrame;
	}

	public static void fill(double[][] src, double[][] dst, int width, int height, int maxRadius) {
		for (int y = 0; y < height; y++) {
			double[] row = src[y];			
			for (int x = 0; x < width; x++) {
				if(!Double.isFinite(row[x])) {
					fill(src, dst, width, height, x, y, maxRadius);
				}
			}
		}
	}

	public static void fill(float[][] src, float[][] dst, int width, int height, int maxRadius) {
		for (int y = 0; y < height; y++) {
			float[] row = src[y];			
			for (int x = 0; x < width; x++) {
				if(!Float.isFinite(row[x])) {
					fill(src, dst, width, height, x, y, maxRadius);
				}
			}
		}
	}

	public static void fillBordered(float[][] src, float[][] dst, int width, int height, int maxRadius) {
		int xmin = maxRadius;
		int ymin = maxRadius;
		int xupper = width - maxRadius;
		int yupper = height - maxRadius;
		int notFilled = 0;
		final int maxNotFilled = 64;
		for (int y = ymin; y < yupper; y++) {
			float[] row = src[y];			
			for (int x = xmin; x < xupper; x++) {
				if(Float.isFinite(row[x])) {
					notFilled = 0;
				} else
					if(notFilled < maxNotFilled) {
						if(fillBordered(src, dst, width, height, x, y, 1, maxRadius)) {
							notFilled = 0;
						} else
							notFilled++;
					} else {
						if(fillBordered(src, dst, width, height, x, y, maxRadius, maxRadius)) {
							notFilled = 0;
							fillBordered(src, dst, width, height, x, y, 1, maxRadius);
						} else
							notFilled++;
					}
			}
		}
	}


	private static void fill(double[][] src, double[][] dst, int width, int height, int x, int y, int maxRadius) {
		int radius = 1;
		while(radius <= maxRadius && !fillG(src, dst, width, height, x, y, radius)) {
			radius++;
			//Logger.info("check " + radius);
		}
	}

	private static void fill(float[][] src, float[][] dst, int width, int height, int x, int y, int maxRadius) {
		int radius = 1;
		while(radius <= maxRadius && !fillG(src, dst, width, height, x, y, radius)) {
			radius++;
			//Logger.info("check " + radius);
		}
	}

	private static boolean fillBordered(float[][] src, float[][] dst, int width, int height, int x, int y, int minRadius, int maxRadius) {
		for (int radius = minRadius; radius <= maxRadius; radius++) {
			if(fillGBordered(src, dst, width, height, x, y, radius)) {
				return true;
			}
		}
		return false;
	}

	private static boolean fillG(double[][] src, double[][] dst, int width, int height, int fx, int fy, int radius) {
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

	private static boolean fillG(float[][] src, float[][] dst, int width, int height, int fx, int fy, int radius) {
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

	private static boolean fillGBordered(float[][] src, float[][] dst, int width, int height, int fx, int fy, int radius) {
		int xmin = fx - radius;
		int ymin = fy - radius;
		int xmax = fx + radius;
		int ymax = fy + radius;

		float cnt = 0;
		float sum = 0;

		{
			float[] row = src[ymin];
			for (int ix = xmin; ix <= xmax; ix++) {
				float v = row[ix];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		{
			float[] row = src[ymax];
			for (int ix = xmin; ix <= xmax; ix++) {
				float v = row[ix];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		{
			for (int iy = ymin + 1; iy < ymax; iy++) {
				float v = src[iy][xmin];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		{
			for (int iy = ymin + 1; iy < ymax; iy++) {
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
