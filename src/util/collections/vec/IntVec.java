package util.collections.vec;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.DoubleConsumer;

public class IntVec {
	//private static final Logger log = LogManager.getLogger();

	private static final int[] DEFAULT_SIZED_EMPTY_ARRAY = {};

	protected int size;
	protected int[] items;
	
	public static <T> IntVec ofOne(int e) {
		return new IntVec(new int[] {e}, 1);
	}
	
	public IntVec() {
		items = DEFAULT_SIZED_EMPTY_ARRAY;
	}

	public IntVec(int initialCapacity) {
		items = new int[initialCapacity];
	}
	
	public IntVec(int[] items) {
		this.items = items;
		this.size = items.length;
	}
	
	public IntVec(int[] items, int size) {
		this.items = items;
		this.size = size;
	}
	
	public void forEach(DoubleConsumer consumer) {
		int len = size;
		int[] data = items;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i]);
		}
	}
	
	public void add(int e) {		
		if (items.length == size) {
			growForOne();
		}
		items[size++] = e;
	}
	
	private void growForOne() {
		if (items == DEFAULT_SIZED_EMPTY_ARRAY) {
			items = new int[10];
		} else {
			int oldLen = size;
			int newLen = oldLen + (oldLen >> 1) + 1;
			int[] newItems = new int[newLen];
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
	
	public int get(int index) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}		
		return this.items[index];       
	}
	
	public int min() {		
		int len = size;
		int[] data = items;
		if(len == 0) {
			throw new NoSuchElementException();
		}
		int min = data[0];
		for (int i = 1; i < len; i++) {
			int v = data[i];
			if(v < min) {
				min = v;
			}
		}
		return min;
	}
	
	public int max() {		
		int len = size;
		int[] data = items;
		if(len == 0) {
			throw new NoSuchElementException();
		}
		int max = data[0];
		for (int i = 1; i < len; i++) {
			int v = data[i];
			if(max < v) {
				max = v;
			}
		}
		return max;
	}
	
	public int[] toArray() {
		return Arrays.copyOf(items, size);
	}
	
	public void clear() {		
		size = 0;
	}
}
