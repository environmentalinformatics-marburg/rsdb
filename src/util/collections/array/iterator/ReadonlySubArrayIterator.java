package util.collections.array.iterator;

import java.util.Objects;
import java.util.function.Consumer;

import util.collections.iterator.ReadonlyListIterator;

public class ReadonlySubArrayIterator<E> implements ReadonlyListIterator<E> {
	
	private final E[] a;
	private final int fromIndex;
	private final int toIndex;

	private int pos;
	
	public ReadonlySubArrayIterator(E[] a, int fromIndex, int toIndex) {
		this.a = Objects.requireNonNull(a);
		if(fromIndex < 0 || toIndex < fromIndex || a.length < toIndex) {
			throw new IndexOutOfBoundsException(""+fromIndex+"  "+toIndex);
		}
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.pos = 0;
	}
	
	public ReadonlySubArrayIterator(E[] a, int fromIndex, int startIndex, int toIndex) {
		this.a = Objects.requireNonNull(a);
		if(fromIndex < 0 || startIndex < fromIndex || toIndex < startIndex || a.length < toIndex) {
			throw new IndexOutOfBoundsException(""+fromIndex+"  "+toIndex+"   "+startIndex);
		}
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.pos = startIndex;
	}
	
	@Override
	public boolean hasNext() {
		return pos < toIndex;
	}
	
	@Override
	public E next() {
		if(pos >= toIndex) {
			throw new IndexOutOfBoundsException(""+fromIndex+"  "+toIndex+"   "+pos);
		}
		return a[pos++];
	}
	
	@Override
	public void forEachRemaining(Consumer<? super E> action) {		
		while(pos < toIndex) {
			action.accept(a[pos++]);
		}
	}

	@Override
	public boolean hasPrevious() {
		return pos > fromIndex;
	}

	@Override
	public E previous() {
		if(pos <= fromIndex) {
			throw new IndexOutOfBoundsException(""+fromIndex+"  "+toIndex+"   "+pos);
		}
		return a[--pos];
	}

	@Override
	public int nextIndex() {
		return pos-toIndex;
	}

	@Override
	public int previousIndex() {
		return (pos-1)-toIndex;
	}	
}
