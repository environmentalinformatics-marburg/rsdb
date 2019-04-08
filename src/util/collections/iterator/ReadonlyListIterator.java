package util.collections.iterator;

import java.util.ListIterator;

public interface ReadonlyListIterator<E> extends ListIterator<E> {
	
	@Override
	public default void remove() {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default void set(E e) {
		throw new UnsupportedOperationException("readonly");		
	}

	@Override
	public default void add(E e) {
		throw new UnsupportedOperationException("readonly");		
	}
}
