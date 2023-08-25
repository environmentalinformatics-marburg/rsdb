package util.collections.vec;

import java.util.Arrays;
import java.util.function.DoubleConsumer;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class DoubleVec {

	private static final double[] DEFAULT_SIZED_EMPTY_ARRAY = {};

	protected int size;
	protected double[] items;
	
	public static <T> DoubleVec ofOne(double e) {
		return new DoubleVec(new double[] {e}, 1);
	}
	
	public DoubleVec() {
		items = DEFAULT_SIZED_EMPTY_ARRAY;
	}

	public DoubleVec(int initialCapacity) {
		items = new double[initialCapacity];
	}
	
	public DoubleVec(double[] items) {
		this.items = items;
		this.size = items.length;
	}
	
	public DoubleVec(double[] items, int size) {
		this.items = items;
		this.size = size;
	}
	
	public void forEach(DoubleConsumer consumer) {
		int len = size;
		double[] data = items;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i]);
		}
	}
	
	public void add(double e) {		
		if (items.length == size) {
			growForOne();
		}
		items[size++] = e;
	}
	
	private void growForOne() {
		if (items == DEFAULT_SIZED_EMPTY_ARRAY) {
			items = new double[10];
		} else {
			int oldLen = size;
			int newLen = oldLen + (oldLen >> 1) + 1;
			double[] newItems = new double[newLen];
			System.arraycopy(this.items, 0, newItems, 0, oldLen);
			items = newItems;
		}
	}
	
	public void sort() {
		Arrays.parallelSort(items, 0, size);
	}
	
	public int size() {
		return size;
	}
	
	public double get(int index) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}		
		return this.items[index];       
	}
	
	public double[] toArray() {
		return Arrays.copyOf(items, size);
	}
	
	public void clear() {		
		size = 0;
	}
	
	public double min() {		
		int n = size;
		double[] data = items;
		if(n == 0) {
			return Double.NaN;
		}
		double min = data[0];
		for (int i = 0; i < n; i++) {
			double v = data[i];
			if(v < min) {
				min = v;
			}
		}
		return min;
	}
	
	public double max() {		
		int n = size;
		double[] data = items;
		if(n == 0) {
			return Double.NaN;
		}
		double max = data[0];
		for (int i = 0; i < n; i++) {
			double v = data[i];
			if(max < v) {
				max = v;
			}
		}
		return max;
	}
	
	public double sum() {		
		int n = size;
		double[] data = items;
		double sum = 0d;
		for (int i = 0; i < n; i++) {
			double v = data[i];
			sum += v;
		}
		return sum;
	}
	
	public double mean() {
		int n = size;
		double[] data = items;
		double sum = 0d;
		for (int i = 0; i < n; i++) {
			double v = data[i];
			sum += v;
		}
		double mean = sum / n;
		return mean;
	}
	
	public double sd() {
		/*int n = size;
		double[] data = items;
		double sum = 0d;
		double qsum = 0d;
		for (int i = 0; i < n; i++) {
			double v = data[i];
			sum += v;
			qsum += v * v;
		}
		double mean = sum / n;
		double sd = Math.sqrt(qsum / n - mean * mean);
		return sd;*/
		return new StandardDeviation().evaluate(items, 0, size);
	}
	
	public double skewness() {
		/*int n = size;
		double[] data = items;
		double sum = 0d;
		double qsum = 0d;
		double csum = 0d;
		for (int i = 0; i < n; i++) {
			double v = data[i];
			sum += v;
			qsum += v * v;
			csum += v * v * v;
		}
		double mean = sum / n;
	    double sd = Math.sqrt(qsum / n - mean * mean);
	    double skewness = (csum - 3 * mean * qsum + 2 * mean * mean * mean) / (sd * sd * sd) / n;
	    return skewness;*/
		return new Skewness().evaluate(items, 0, size);
	}
	
	public double kurtosis() {
		return new Kurtosis().evaluate(items, 0, size);
	}
	
	public double excess_kurtosis() {
		return kurtosis() - 3d;
	}
	
	public double cv() {
		int n = size;
		double[] data = items;
		double sum = 0d;
		double qsum = 0d;
		for (int i = 0; i < n; i++) {
			double v = data[i];
			sum += v;
			qsum += v * v;
		}
		double mean = sum / n;
	    double cv = Math.sqrt(qsum / n - mean * mean) / n;
	    return cv;
	}

	public DoubleVec div(DoubleVec v) {
		int n = Math.min(size, v.size);
		double[] data = items;
		double[] dataV = v.items;
		double[] r = new double[n];
		for (int i = 0; i < n; i++) {
			double vv = data[i];
			double vV = dataV[i];
			double rV = vv / vV;
			r[i] = rV;
		}
		return new DoubleVec(r);
	}
}
