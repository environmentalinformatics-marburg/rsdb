package rasterdb.node;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.BandProcessor;
import util.frame.DoubleFrame;

public class ProcessorNode_gap_filling extends ProcessorNode {
	static final Logger log = LogManager.getLogger();

	private final ProcessorNode node;
	private final int maxRadius;

	public ProcessorNode_gap_filling(ProcessorNode node, int maxRadius) {
		this.node = node;
		this.maxRadius = maxRadius;
	}

	@Override
	public DoubleFrame[] process(BandProcessor processor) {
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

	private static void fill(double[][] src, double[][] dst, int width, int height, int x, int y, int maxRadius) {
		int radius = 1;
		while(radius <= maxRadius && !fillG(src, dst, width, height, x, y, radius)) {
			radius++;
			//log.info("check " + radius);
		}
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
			//log.info("fill "+fx+" "+fy+" "+cnt);
			dst[fy][fx] = sum / cnt;
			return true;
		}
		return false;
	}

}
