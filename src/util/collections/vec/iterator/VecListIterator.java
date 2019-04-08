package util.collections.vec.iterator;

import util.collections.array.iterator.ReadonlyArrayIterator;
import util.collections.vec.Vec;

public class VecListIterator<T> extends ReadonlyArrayIterator<T> {

	protected final Vec<T> vec;

	public VecListIterator(Vec<T> vec) {
		super(vec.items(), 0, vec.size());
		this.vec = vec;
	}

	public VecListIterator(Vec<T> vec, int curIndex) {
		super(vec.items(), curIndex, vec.size());
		this.vec = vec;
	}

	@Override
	public void add(T e) {
		vec.add(pos, e);
	}

	@Override
	public void remove() {
		int npos = pos - 1;
		vec.removeFast(npos);
		pos = npos;
		len = vec.size();
	}

	@Override
	public void set(T e) {
		vec.set(pos, e);
	}
}
