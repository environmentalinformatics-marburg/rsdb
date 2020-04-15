package util.image;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Timer;

public class RangerDouble {
	static final Logger log = LogManager.getLogger();

	public static double[] range(double[][] data) {
		int height = data.length;
		int yStart = 0;
		int yEnd = height - 1;
		return range(data, yStart, yEnd);
	}
	
	public static double[] rangeSync(double[][][] data) {
		int bands = data.length;
		int lines = data[0].length;
		int columns = data[0][0].length;
		if(bands == 0 || lines == 0 || columns == 0) {
			return null;
		}
		int len = bands * lines * columns;
		double[] stat = new double[len];		
		int cnt = 0;
		for(int b = 0; b < bands; b++) {
			double[][] layer = data[b];
			for(int y = 0; y < lines; y++) {
				double[] row = layer[y];
				for(int x = 0; x < columns; x++) {
					double v = row[x];
					if(Double.isFinite(v)) {
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
		double median = (cnt % 2 == 0) ? ((stat[cntd2 - 1] + stat[cntd2]) / 2) : stat[cntd2];
		return new double[] {stat[lower], stat[upper], median};		
	}

	public static double[] range_full(double[][] data, int yStart, int yEnd) {		
		int width = data[0].length;
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int y = yStart; y <= yEnd; y++) {
			double[] row = data[y];
			for (int x = 0; x < width; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					if(v < min) {
						min = v;
					}
					if(max < v) {
						max = v;
					}
				}
			}
		}
		return min == Double.POSITIVE_INFINITY ? null : new double[] {min, max};
	}

	public static double[] range_stat(double[][] data, int yStart, int yEnd) {		
		int width = data[0].length;
		int h = yEnd - yStart + 1;
		int len = h * width;
		double[] stat = new double[len];		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		int pos = 0;
		for (int y = yStart; y <= yEnd; y++) {
			double[] row = data[y];
			System.arraycopy(row, 0, stat, pos, width);
			pos += width;
		}
		DescriptiveStatistics desc = new DescriptiveStatistics(stat);
		min = desc.getPercentile(0.5);
		max = desc.getPercentile(99.5);
		return min == Double.POSITIVE_INFINITY ? null : new double[] {min, max};
	}
	
	public static double[] range(double[][] data, int yStart, int yEnd) {
		if(data.length == 0 || data[0].length == 0) {
			return null;
		}
		int width = data[0].length;
		int h = yEnd - yStart + 1;
		int len = h * width;
		double[] stat = new double[len];		
		int cnt = 0;
		for (int y = yStart; y <= yEnd; y++) {
			double[] row = data[y];			
			for (int x = 0; x < width; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
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
		double median = (cnt % 2 == 0) ? ((stat[cntd2 - 1] + stat[cntd2]) / 2) : stat[cntd2];
		return new double[] {stat[lower], stat[upper], median};		
	}

	public static double[] getRange(double[][] data, double[] setRange) {
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
			//log.info(Timer.stop("Ranger"));
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
			med = (min + max) / 2;
		}
		return new double[] {min, max, med};
	}
	
	public static double[] getRangeSync(double[][][] data, double[] setRange) {
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
			med = (min + max) / 2;
		}
		return new double[] {min, max, med};
	}
	
	public static double getGamma(double[] range, double gamma) {
		if(Double.isFinite(gamma)) {
			return gamma;
		}
		if(range.length < 3) {
			log.warn("no median");
			return 2.0;
		}
		double median = range[2];
		if(range[0] == median) {
			median = (range[0] + range[1]) / 2;
			log.warn("median error");
			return 2.0;
		}
		double f = (median - range[0]) / (range[1] - range[0]);
		double g = Math.log(f) / Math.log(0.5);
		return g;
	}
	
	public static double getGamma(int[] range, double gamma) {
		double[] r = new double[range.length];
		for (int i = 0; i < r.length; i++) {
			r[i] = range[i];
		}
		return getGamma(r, gamma);
	}

}
