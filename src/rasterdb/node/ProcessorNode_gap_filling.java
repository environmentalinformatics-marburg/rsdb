package rasterdb.node;

import rasterdb.FrameProducer;
import util.Filler;
import util.FillerBordered;
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
		while(radius <= maxRadius && !Filler.fillG(src, dst, width, height, x, y, radius)) {
			radius++;
			//Logger.info("check " + radius);
		}
	}

	private static void fill(float[][] src, float[][] dst, int width, int height, int x, int y, int maxRadius) {
		int radius = 1;
		while(radius <= maxRadius && !Filler.fillG(src, dst, width, height, x, y, radius)) {
			radius++;
			//Logger.info("check " + radius);
		}
	}

	private static boolean fillBordered(float[][] src, float[][] dst, int width, int height, int x, int y, int minRadius, int maxRadius) {
		for (int radius = minRadius; radius <= maxRadius; radius++) {
			if(FillerBordered.fillGBorderedSquare(src, dst, width, height, x, y, radius)) {
				return true;
			}
		}
		return false;
	}
}
