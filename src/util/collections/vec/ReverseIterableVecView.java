package util.collections.vec;

import java.util.Iterator;

public class ReverseIterableVecView<T> implements Iterable<T> {

	private final Vec<T> vec;

	public ReverseIterableVecView(Vec<T> vec) {
		this.vec = vec;	
	}

	@Override
	public Iterator<T> iterator() {
		return vec.reverseIterator();
	}
}
