package util.collections.array.iterator;

import java.util.Objects;
import java.util.function.Consumer;

import util.collections.iterator.ReadonlyListIterator;

public class ReadonlyArrayReverseIterator<E> implements ReadonlyListIterator<E> {
	
	protected final E[] a;
	protected int pos;
	protected int len;
	
	public ReadonlyArrayReverseIterator(E[] a) {
		this.a = Objects.requireNonNull(a);
		this.pos = a.length;
		this.len = a.length;
	}
	
	public ReadonlyArrayReverseIterator(E[] a, int prevIndex, int size) {
		this.a = Objects.requireNonNull(a);
		if(prevIndex < 0 || size < prevIndex || a.length < size) {
			throw new IndexOutOfBoundsException(prevIndex + "   " + size);
		}
		this.pos = prevIndex;
		this.len = size;
	}
	
	@Override
	public boolean hasNext() {
		return pos > 0;
	}
	
	@Override
	public boolean hasPrevious() {
		return pos < len;		
	}
	
	
	@Override
	public E next() {
		return a[--pos];
	}
	
	@Override
	public E previous() {
		return a[pos++];		
	}
	
	
	@Override
	public int nextIndex() {
		return pos - 1;
	}

	@Override
	public int previousIndex() {
		return pos;		
	}	
	
	@Override
	public void forEachRemaining(Consumer<? super E> action) {		
		while(pos > 0) {
			action.accept(a[--pos]);
		}
	}
	
	public ReadonlyArrayIterator<E> asReverseIterator() {
		return new ReadonlyArrayIterator<E>(a, pos, len);
	}
}