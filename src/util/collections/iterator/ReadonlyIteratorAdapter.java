package util.collections.iterator;

import java.util.Iterator;
import java.util.function.Consumer;

public class ReadonlyIteratorAdapter<T> implements Iterator<T> {

	private final Iterator<T> it;

	public ReadonlyIteratorAdapter(Iterator<T> it) {
		this.it = it;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public T next() {
		return it.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		it.forEachRemaining(action);
	}

	@Override
	public int hashCode() {
		return it.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return it.equals(obj);
	}

	@Override
	public String toString() {
		return it.toString();
	}
}
