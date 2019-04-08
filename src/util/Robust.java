package util;

import java.util.Arrays;

/**
 * Get mean of a set of values, excluding outliers.
 * @author woellauer
 *
 */
public class Robust {
	/*

1
x

1 2
x x

1 2 3
o o x

1 2 3 4
o o x x

1 2 3 4 5
o o o x x

1 2 3 4 5 6
o o o x x x

1 2 3 4 5 6 7
o o o o x x x

	 */
	/**
	 * Get mean of a set of values, excluding outliers.
	 * @param acc
	 * @param cnt use first count of values.
	 * @return
	 */
	public static double getMostLikely(double[] acc, int cnt) {
		switch(cnt) {
		case 0:
		case 1:
		case 2:
			return Double.NaN;
		case 3:
			return getMostLikely3(acc);
		case 4:
			return getMostLikely4(acc);
		case 5:
		case 6:
			return robustValue3(acc,cnt);
		default:
			return robustValue4(acc,cnt);
		}
	}

	/**
	 * Get average of two nearest values of three.
	 * @param acc
	 * @return
	 */
	public static double getMostLikely3(double[] acc) {
		Arrays.sort(acc,0,2);
		double a = acc[0];
		double b = acc[1];
		double c = acc[2];
		double ab = Math.abs(a-b);
		double bc = Math.abs(b-c);
		if(ab<=bc) {
			return (a+b)/2;
		} else {
			return (b+c)/2;
		}
	}

	/**
	 * Get average of two nearest values of four.
	 * @param acc
	 * @return
	 */
	public static double getMostLikely4(double[] acc) {
		Arrays.sort(acc,0,3);
		double a = acc[0];
		double b = acc[1];
		double c = acc[2];
		double d = acc[3];		
		double ab = Math.abs(a-b);
		double bc = Math.abs(b-c);
		double cd = Math.abs(c-d);
		if(ab<=bc) {
			if(ab<=cd) {
				return (a+b)/2;
			} else {
				return (c+d)/2;
			}
		} else {
			if(bc<=cd) {
				return (b+c)/2;
			} else {
				return (c+d)/2;
			}
		}
	}

	/**
	 * Get average of three nearest points out of n points.
	 * @param acc
	 * @param cnt
	 * @return
	 */
	public static double robustValue3(double[] acc, int cnt) {
		Arrays.sort(acc,0,cnt);
		int max = cnt-2;
		double bestAvg = Double.MAX_VALUE;
		double minErr = Double.MAX_VALUE;
		for(int i=0;i<max;i++) {
			double avg = (acc[i]+acc[i+1]+acc[i+2])/3;
			double err = Math.abs(acc[i]-avg)+Math.abs(acc[i+1]-avg)+Math.abs(acc[i+2]-avg);
			if(err<minErr) {
				minErr = err;
				bestAvg = avg;
			}
		}
		return bestAvg;
	}

	/**
	 * Get average of four nearest points out of n points.
	 * @param acc
	 * @param cnt
	 * @return
	 */
	public static double robustValue4(double[] acc, int cnt) {
		Arrays.sort(acc,0,cnt);
		int max = cnt-3;
		double bestAvg = Double.MAX_VALUE;
		double minErr = Double.MAX_VALUE;
		for(int i=0;i<max;i++) {
			double avg = (acc[i]+acc[i+1]+acc[i+2]+acc[i+3])/4;
			double err = Math.abs(acc[i]-avg)+Math.abs(acc[i+1]-avg)+Math.abs(acc[i+2]-avg)+Math.abs(acc[i+3]-avg);
			if(err<minErr) {
				minErr = err;
				bestAvg = avg;
			}
		}
		return bestAvg;
	}
}
