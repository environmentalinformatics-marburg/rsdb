package util.collections.array.iterator;

import java.util.Objects;
import java.util.function.Consumer;

import util.collections.iterator.ReadonlyListIterator;

public class ReadonlyArrayIterator<E> implements ReadonlyListIterator<E> {
	
	protected final E[] a;
	protected int pos;
	protected int len;
	
	public ReadonlyArrayIterator(E[] a) {
		this.a = Objects.requireNonNull(a);
		this.pos = 0;
		this.len = a.length;
	}
	
	public ReadonlyArrayIterator(E[] a, int index, int size) {
		this.a = Objects.requireNonNull(a);
		if(index < 0 || size < index || a.length < size) {
			throw new IndexOutOfBoundsException(index + "   " + size);
		}
		this.pos = index;
		this.len = size;
	}
	
	@Override
	public boolean hasNext() {
		return pos < len;
	}
	
	@Override
	public E next() {
		return a[pos++];
	}
	
	@Override
	public void forEachRemaining(Consumer<? super E> action) {		
		while(pos < len) {
			action.accept(a[pos++]);
		}
	}

	@Override
	public boolean hasPrevious() {
		return pos > 0;
	}

	@Override
	public E previous() {
		return a[--pos];
	}

	@Override
	public int nextIndex() {
		return pos;
	}

	@Override
	public int previousIndex() {
		return pos-1;
	}
	
	public ReadonlyArrayReverseIterator<E> asReverseIterator() {
		return new ReadonlyArrayReverseIterator<E>(a, pos, len);
	}
}