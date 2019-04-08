package util.image;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Timer;

public class RangerFloat {
	static final Logger log = LogManager.getLogger();

	public static double[] range(float[][] data) {
		int height = data.length;
		int yStart = 0;
		int yEnd = height - 1;
		return range(data, yStart, yEnd);
	}

	public static double[] rangeSync(float[][][] data) {
		int bands = data.length;
		int lines = data[0].length;
		int columns = data[0][0].length;
		if(bands == 0 || lines == 0 || columns == 0) {
			return null;
		}
		int len = bands * lines * columns;
		float[] stat = new float[len];		
		int cnt = 0;
		for(int b = 0; b < bands; b++) {
			float[][] layer = data[b];
			for(int y = 0; y < lines; y++) {
				float[] row = layer[y];
				for(int x = 0; x < columns; x++) {
					float v = row[x];
					if(Float.isFinite(v)) {
						stat[cnt++] = row[x];
					}
				}
			}
		}
		if(cnt == 0) {
			return null;
		}
		Arrays.parallelSort(stat, 0, cnt);
		long pro = (long) cnt;
		int lower = (int) ((pro * 5) / 1000);
		int upper = (int) ((pro * 995) / 1000);
		int cntd2 = cnt >>> 1;
			float median = (cnt % 2 == 0) ? ((stat[cntd2 - 1] + stat[cntd2]) / 2) : stat[cntd2];
			return new double[] {stat[lower], stat[upper], median};
	}

	public static double[] range_full(float[][] data, int yStart, int yEnd) {		
		int width = data[0].length;
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (int y = yStart; y <= yEnd; y++) {
			float[] row = data[y];
			for (int x = 0; x < width; x++) {
				float v = row[x];
				if(Float.isFinite(v)) {
					if(v < min) {
						min = v;
					}
					if(max < v) {
						max = v;
					}
				}
			}
		}
		return min == Float.POSITIVE_INFINITY ? null : new double[] {min, max};
	}

	public static double[] range_stat(float[][] data, int yStart, int yEnd) {		
		int width = data[0].length;
		int h = yEnd - yStart + 1;
		int len = h * width;
		double[] stat = new double[len];		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		int pos = 0;
		for (int y = yStart; y <= yEnd; y++) {
			float[] row = data[y];			
			for (int x = 0; x < width; x++) {
				stat[pos++] = row[x];
			}
		}
		DescriptiveStatistics desc = new DescriptiveStatistics(stat);
		min = desc.getPercentile(0.5);
		max = desc.getPercentile(99.5);
		return min == Double.POSITIVE_INFINITY ? null : new double[] {min, max};
	}

	public static double[] range(float[][] data, int yStart, int yEnd) {
		if(data.length == 0 || data[0].length == 0) {
			return null;
		}
		int width = data[0].length;
		int h = yEnd - yStart + 1;
		int len = h * width;
		float[] stat = new float[len];		
		int cnt = 0;
		for (int y = yStart; y <= yEnd; y++) {
			float[] row = data[y];			
			for (int x = 0; x < width; x++) {
				float v = row[x];
				if(Float.isFinite(v)) {
					stat[cnt++] = row[x];
				}
			}
		}
		if(cnt == 0) {
			return null;
		}
		Arrays.parallelSort(stat, 0, cnt);
		long pro = (long) cnt;
		int lower = (int) ((pro * 5) / 1000);
		int upper = (int) ((pro * 995) / 1000);
		int cntd2 = cnt >>> 1;
		float median = (cnt % 2 == 0) ? ((stat[cntd2 - 1] + stat[cntd2]) / 2) : stat[cntd2];
		return new double[] {stat[lower], stat[upper], median};
	}

	public static double[] getRange(float[][] data, double[] setRange) {
		double min = Double.NEGATIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double med = Double.NEGATIVE_INFINITY;
		if(setRange != null) {
			min = Double.isFinite(setRange[0]) ? setRange[0] : Double.NEGATIVE_INFINITY;
			max = Double.isFinite(setRange[1]) ? setRange[1] : Double.NEGATIVE_INFINITY;
		}
		if(min == Double.NEGATIVE_INFINITY || max == Double.NEGATIVE_INFINITY) {
			Timer.start("Ranger");
			double[] r = range(data);
			log.info(Timer.stop("Ranger"));
			if(r != null) {
				min = r[0];
				max = r[1];
				med = r[2];
			}
		}
		if(min == Double.NEGATIVE_INFINITY) {
			min = 0;
		}
		if(max == Double.NEGATIVE_INFINITY || max == min) {
			max = min + 1;
		}
		if(med == Double.NEGATIVE_INFINITY) {
			log.info("set med");
			med = (min + max) / 2;
		}
		return new double[] {min, max, med};
	}

	public static double[] getRangeSync(float[][][] data, double[] setRange) {
		double min = Double.NEGATIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double med = Double.NEGATIVE_INFINITY;
		if(setRange != null) {
			min = Double.isFinite(setRange[0]) ? setRange[0] : Double.NEGATIVE_INFINITY;
			max = Double.isFinite(setRange[1]) ? setRange[1] : Double.NEGATIVE_INFINITY;
		}
		if(min == Double.NEGATIVE_INFINITY || max == Double.NEGATIVE_INFINITY) {
			Timer.start("Ranger");
			double[] r = rangeSync(data);
			log.info(Timer.stop("Ranger"));
			if(r != null) {
				min = r[0];
				max = r[1];
				med = r[2];
			}
		}
		if(min == Double.NEGATIVE_INFINITY) {
			min = 0;
		}
		if(max == Double.NEGATIVE_INFINITY || max == min) {
			max = min + 1;
		}
		if(med == Double.NEGATIVE_INFINITY) {
			log.info("set med");
			med = (min + max) / 2;
		}
		return new double[] {min, max, med};
	}

}
