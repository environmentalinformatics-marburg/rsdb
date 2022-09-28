package util.collections;

import java.util.AbstractCollection;
import java.util.Iterator;

public class SwitchingCollection<E> extends AbstractCollection<E> implements Iterator<E> {
	
	boolean hasElements = false;

	@Override
	public Iterator<E> iterator() {
		return this;
	}

	@Override
	public int size() {
		return hasElements ? 1 : 0;
	}

	@Override
	public boolean add(E e) {
		hasElements = true;
		return true;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return !hasElements;
	}

	@Override
	public void clear() {
		hasElements = false;
	}

	@Override
	public String toString() {
		return hasElements ? "Some Elements collected." : "No Elements collected.";
	}

	@Override
	public boolean hasNext() {
		return hasElements;
	}

	@Override
	public E next() {
		return null;
	}
}
