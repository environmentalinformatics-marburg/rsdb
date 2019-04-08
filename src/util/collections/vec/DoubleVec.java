package util.collections.vec;

import java.util.Arrays;
import java.util.function.DoubleConsumer;

public class DoubleVec {
	//private static final Logger log = LogManager.getLogger();

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
	
	public double min() {		
		int len = size;
		double[] data = items;
		if(len == 0) {
			return Double.NaN;
		}
		double min = data[0];
		for (int i = 1; i < len; i++) {
			double v = data[i];
			if(v < min) {
				min = v;
			}
		}
		return min;
	}
	
	public double max() {		
		int len = size;
		double[] data = items;
		if(len == 0) {
			return Double.NaN;
		}
		double max = data[0];
		for (int i = 1; i < len; i++) {
			double v = data[i];
			if(max < v) {
				max = v;
			}
		}
		return max;
	}
	
	public double[] toArray() {
		return Arrays.copyOf(items, size);
	}
	
	public void clear() {		
		size = 0;
	}
}
