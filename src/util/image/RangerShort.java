package util.image;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Timer;

public class RangerShort {
	static final Logger log = LogManager.getLogger();

	public static short[] range(short[][] data, short na) {
		int height = data.length;
		int yStart = 0;
		int yEnd = height - 1;
		return range(data, na, yStart, yEnd);
	}

	public static short[] rangeSync(short[][][] data, short[] nas) {
		int bands = data.length;
		int lines = data[0].length;
		int columns = data[0][0].length;
		if(bands == 0 || lines == 0 || columns == 0) {
			return null;
		}
		int len = bands * lines * columns;
		short[] stat = new short[len];		
		int cnt = 0;
		for(int b = 0; b < bands; b++) {
			short[][] layer = data[b];
			short na = nas[b];
			for(int y = 0; y < lines; y++) {
				short[] row = layer[y];
				for(int x = 0; x < columns; x++) {
					short v = row[x];
					if(v != na) {
						stat[cnt++] = v;
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
		short median = (cnt % 2 == 0) ? (short)(((((int)stat[cntd2 - 1]) + ((int)stat[cntd2])) >> 1)) : stat[cntd2];
		return new short[] {stat[lower], stat[upper], median};
	}

	public static short[] range_full(short[][] data, short na, int yStart, int yEnd) {		
		int width = data[0].length;
		short min = Short.MAX_VALUE;
		short max = Short.MIN_VALUE;
		for (int y = yStart; y <= yEnd; y++) {
			short[] row = data[y];
			for (int x = 0; x < width; x++) {
				short v = row[x];
				if(v != na) {
					if(v < min) {
						min = v;
					}
					if(max < v) {
						max = v;
					}
				}
			}
		}
		return min == Short.MAX_VALUE ? null : new short[] {min, max};
	}

	public static short[] range_stat(short[][] data, short na, int yStart, int yEnd) {		
		int width = data[0].length;
		int h = yEnd - yStart + 1;
		int len = h * width;
		double[] stat = new double[len];		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		int pos = 0;
		for (int y = yStart; y <= yEnd; y++) {
			short[] row = data[y];
			for (int x = 0; x < width; x++) {
				short v = row[x];
				stat[pos++] = v == na ? Double.NaN : v;
			}
		}
		DescriptiveStatistics desc = new DescriptiveStatistics(stat);
		min = desc.getPercentile(0.5);
		max = desc.getPercentile(99.5);
		return Double.isFinite(min) && Double.isFinite(max) ? new short[] {(short) min, (short) max} : null;
		//return Double.isFinite(min) && Double.isFinite(max) ? new short[] {0, (short) max} : null;
	}

	public static short[] range(short[][] data, short na, int yStart, int yEnd) {
		if(data.length == 0 || data[0].length == 0) {
			return null;
		}
		int width = data[0].length;
		int h = yEnd - yStart + 1;
		int len = h * width;
		short[] stat = new short[len];		
		int cnt = 0;
		for (int y = yStart; y <= yEnd; y++) {
			short[] row = data[y];
			for (int x = 0; x < width; x++) {
				short v = row[x];
				if(v != na) {
					stat[cnt++] = v;
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
		short median = (cnt % 2 == 0) ? (short)(((((int)stat[cntd2 - 1]) + ((int)stat[cntd2])) >> 1)) : stat[cntd2];
		return new short[] {stat[lower], stat[upper], median};
	}

	public static int[] getRange(short[][] data, short na, double[] setRange) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MIN_VALUE;
		int med = Integer.MIN_VALUE;
		if(setRange != null) {
			min = Double.isFinite(setRange[0]) ? (int)setRange[0] : Integer.MIN_VALUE;
			max = Double.isFinite(setRange[1]) ? (int)setRange[1] : Integer.MIN_VALUE;
		}
		if(min == Integer.MIN_VALUE || max == Integer.MIN_VALUE) {
			Timer.start("Ranger");
			short[] r = RangerShort.range(data, na);
			//log.info(Timer.stop("Ranger"));
			if(r != null) {
				min = r[0];
				max = r[1];
				med = r[2];
			}
		}
		if(min == Integer.MIN_VALUE) {
			min = 0;
		}
		if(max == Integer.MIN_VALUE || max == min) {
			max = min + 1;
		}
		if(med == Integer.MIN_VALUE) {
			med = (min + max) / 2;
		}
		return new int[] {min, max, med};
	}

	public static int[] getRangeSync(short[][][] data, short[] na, double[] setRange) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MIN_VALUE;
		int med = Integer.MIN_VALUE;
		if(setRange != null) {
			min = Double.isFinite(setRange[0]) ? (int)setRange[0] : Integer.MIN_VALUE;
			max = Double.isFinite(setRange[1]) ? (int)setRange[1] : Integer.MIN_VALUE;
		}
		if(min == Integer.MIN_VALUE || max == Integer.MIN_VALUE) {
			Timer.start("Ranger");
			short[] r = RangerShort.rangeSync(data, na);
			//log.info(Timer.stop("Ranger"));
			if(r != null) {
				min = r[0];
				max = r[1];
				med = r[2];
			}
		}
		if(min == Integer.MIN_VALUE) {
			min = 0;
		}
		if(max == Integer.MIN_VALUE || max == min) {
			max = min + 1;
		}
		if(med == Integer.MIN_VALUE) {
			med = (min + max) / 2;
		}
		return new int[] {min, max, med};
	}

}
