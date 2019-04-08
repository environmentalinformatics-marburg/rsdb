package util.collections.vec;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import util.collections.ReadonlyList;
import util.collections.array.iterator.ReadonlyArrayIterator;

public class ReadonlyVecSubView<T> extends AbstractList<T> implements ReadonlyList<T> {

	private final Vec<T> vec;	
	private final int offset;
	private final int size;

	public ReadonlyVecSubView(Vec<T> vec, int fromIndex, int toIndex) {
		this.vec = Objects.requireNonNull(vec);
		if(fromIndex < 0 || toIndex < fromIndex || toIndex > vec.size) {
			throw new IndexOutOfBoundsException(""+fromIndex+"  "+toIndex);
		}
		this.offset = fromIndex;
		this.size = toIndex - fromIndex;
	}

	@Override
	public T get(int index) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}
		return vec.get(offset + index);       
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Iterator<T> iterator() {
		return new ReadonlyArrayIterator<T>(vec.items, offset, offset + size);
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		int len = offset + size;
		T[] data = vec.items;
		for (int i = offset; i < len; i++) {
			consumer.accept(data[i]);
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		return Arrays.spliterator(vec.items, offset, offset + size);
	}

	@Override
	public ReadonlyVecSubView<T> subList(int fromIndex, int toIndex) {
		if(this.size <= toIndex) {
			throw new IndexOutOfBoundsException();
		}
		return new ReadonlyVecSubView<T>(vec, offset + fromIndex, offset + toIndex);
	}

	@Override
	public T[] toArray(IntFunction<T[]> generator) {
		return vec.toArray(generator, offset, offset + offset);
	}
}
