package pointcloud;

import java.util.function.IntConsumer;

import com.googlecode.javaewah.datastructure.BitSet;

import util.BitSetUtil;

public class ColumnsUtil {	
	public static double[] transform(int[] x, int len, double xscale, double xoff) {
		double[] rx = new double[len];		
		for (int i = 0; i < len; i++) {
			rx[i] = (x[i] / xscale) + xoff;
		}
		return rx;
	}

	/*public static double[] filterTransform(int[] x, int len, BitSet mask, int size, double xscale, double xoff) {
		double[] rx = new double[size];
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				rx[cnt++] = (x[i] / xscale) + xoff;
			}
		}
		return rx;
	}*/

	private static class ApplyFilterTransformScaleOff implements IntConsumer {
		private final int[] x;
		private final double xscale;
		private final double xoff;
		private final double[] rx;
		private int cnt;
		public ApplyFilterTransformScaleOff(int[] x, double xscale, double xoff, double[] rx) {
			this.x = x;
			this.xscale = xscale;
			this.xoff = xoff;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			rx[cnt++] = (x[i] / xscale) + xoff;			
		}
	}

	public static double[] filterTransform(int[] x, int len, BitSet mask, int size, double xscale, double xoff) {
		double[] rx = new double[size];
		BitSetUtil.forEach(mask, new ApplyFilterTransformScaleOff(x, xscale, xoff, rx));
		return rx;
	}

	public static double[] transform(int[] x, int len, double xscale) {
		double[] rx = new double[len];		
		for (int i = 0; i < len; i++) {
			rx[i] = x[i] / xscale;
		}
		return rx;
	}

	/*public static double[] filterTransform(int[] x, int len, BitSet mask, int size, double xscale) {
		double[] rx = new double[size];
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				rx[cnt++] = x[i] / xscale;
			}
		}
		return rx;
	}*/
	
	private static class ApplyFilterTransformScale implements IntConsumer {
		private final int[] x;
		private final double xscale;
		private final double[] rx;
		private int cnt;
		public ApplyFilterTransformScale(int[] x, double xscale, double[] rx) {
			this.x = x;
			this.xscale = xscale;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			rx[cnt++] = x[i] / xscale;			
		}
	}
	
	public static double[] filterTransform(int[] x, int len, BitSet mask, int size, double xscale) {
		double[] rx = new double[size];
		BitSetUtil.forEach(mask, new ApplyFilterTransformScale(x, xscale, rx));
		return rx;
	}

	/*public static java.util.BitSet filter(java.util.BitSet x, int len, BitSet mask, int size) {
		java.util.BitSet rx = new java.util.BitSet(size);
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				if(x.get(i)) {
					rx.set(cnt);
				}
				cnt++;
			}
		}
		return rx;
	}*/
	
	private static class ApplyFilterBitSet implements IntConsumer {
		private final java.util.BitSet x;
		private final java.util.BitSet rx;
		private int cnt;
		public ApplyFilterBitSet(java.util.BitSet x, java.util.BitSet rx) {
			this.x = x;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			if(x.get(i)) {
				rx.set(cnt);
			}
			cnt++;	
		}
	}

	public static java.util.BitSet filter(java.util.BitSet x, int len, BitSet mask, int size) {
		java.util.BitSet rx = new java.util.BitSet(size);
		BitSetUtil.forEach(mask, new ApplyFilterBitSet(x, rx));
		return rx;
	}

	/*public static double[] filter(double[] x, int len, BitSet mask, int size) {
		double[] rx = new double[size];
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				rx[cnt++] = x[i];
			}
		}
		return rx;
	}*/
	
	private static class ApplyFilterDouble implements IntConsumer {
		private final double[] x;
		private final double[] rx;
		private int cnt;
		public ApplyFilterDouble(double[] x, double[] rx) {
			this.x = x;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			rx[cnt++] = x[i];			
		}
	}

	public static double[] filter(double[] x, int len, BitSet mask, int size) {
		double[] rx = new double[size];
		BitSetUtil.forEach(mask, new ApplyFilterDouble(x, rx));
		return rx;
	}

	/*public static long[] filter(long[] x, int len, BitSet mask, int size) {
		long[] rx = new long[size];
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				rx[cnt++] = x[i];
			}
		}
		return rx;
	}*/
	
	private static class ApplyFilterLong implements IntConsumer {
		private final long[] x;
		private final long[] rx;
		private int cnt;
		public ApplyFilterLong(long[] x, long[] rx) {
			this.x = x;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			rx[cnt++] = x[i];			
		}
	}

	public static long[] filter(long[] x, int len, BitSet mask, int size) {
		long[] rx = new long[size];
		BitSetUtil.forEach(mask, new ApplyFilterLong(x, rx));
		return rx;
	}

	/*public static char[] filter(char[] x, int len, BitSet mask, int size) {
		char[] rx = new char[size];
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				rx[cnt++] = x[i];
			}
		}
		return rx;
	}*/
	
	private static class ApplyFilterChar implements IntConsumer {
		private final char[] x;
		private final char[] rx;
		private int cnt;
		public ApplyFilterChar(char[] x, char[] rx) {
			this.x = x;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			rx[cnt++] = x[i];			
		}
	}

	public static char[] filter(char[] x, int len, BitSet mask, int size) {
		char[] rx = new char[size];
		BitSetUtil.forEach(mask, new ApplyFilterChar(x, rx));
		return rx;
	}
	
	/*public static byte[] filter(byte[] x, int len, BitSet mask, int size) {
		byte[] rx = new byte[size];
		int cnt = 0;			
		for (int i = 0; i < len; i++) {
			if(mask.get(i)) {
				rx[cnt++] = x[i];
			}
		}
		return rx;
	}*/
	
	private static class ApplyFilterByte implements IntConsumer {
		private final byte[] x;
		private final byte[] rx;
		private int cnt;
		public ApplyFilterByte(byte[] x, byte[] rx) {
			this.x = x;
			this.rx = rx;
		}
		@Override
		public void accept(int i) {
			rx[cnt++] = x[i];			
		}
	}

	public static byte[] filter(byte[] x, int len, BitSet mask, int size) {
		byte[] rx = new byte[size];
		BitSetUtil.forEach(mask, new ApplyFilterByte(x, rx));
		return rx;
	}
}
