package util.collections.vec;

import java.util.Arrays;

import util.yaml.YamlMap.FloatConsumer;

public class FloatVec {

	private static final float[] DEFAULT_SIZED_EMPTY_ARRAY = {};

	public int size;
	public float[] items;
	
	public static <T> FloatVec ofOne(float e) {
		return new FloatVec(new float[] {e}, 1);
	}
	
	public FloatVec() {
		items = DEFAULT_SIZED_EMPTY_ARRAY;
	}

	public FloatVec(int initialCapacity) {
		items = new float[initialCapacity];
	}
	
	public FloatVec(float[] items) {
		this.items = items;
		this.size = items.length;
	}
	
	public FloatVec(float[] items, int size) {
		this.items = items;
		this.size = size;
	}
	
	public void forEach(FloatConsumer consumer) {
		int len = size;
		float[] data = items;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i]);
		}
	}
	
	public void add(float e) {		
		if (items.length == size) {
			growForOne();
		}
		items[size++] = e;
	}
	
	private void growForOne() {
		if (items == DEFAULT_SIZED_EMPTY_ARRAY) {
			items = new float[10];
		} else {
			int oldLen = size;
			int newLen = oldLen + (oldLen >> 1) + 1;
			float[] newItems = new float[newLen];
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
	
	public float get(int index) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}		
		return this.items[index];       
	}
	
	public float min() {		
		int len = size;
		float[] data = items;
		if(len == 0) {
			return Float.NaN;
		}
		float min = data[0];
		for (int i = 1; i < len; i++) {
			float v = data[i];
			if(v < min) {
				min = v;
			}
		}
		return min;
	}
	
	public float max() {		
		int len = size;
		float[] data = items;
		if(len == 0) {
			return Float.NaN;
		}
		float max = data[0];
		for (int i = 1; i < len; i++) {
			float v = data[i];
			if(max < v) {
				max = v;
			}
		}
		return max;
	}
	
	public float[] toArray() {
		return Arrays.copyOf(items, size);
	}
	
	public void clear() {		
		size = 0;
	}
}