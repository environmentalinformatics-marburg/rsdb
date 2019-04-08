package util.collections.vec;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import util.collections.ReadonlyList;

public class ReadonlyVecView<T> implements ReadonlyList<T> {
	
	private final Vec<T> vec;
	
	public ReadonlyVecView(Vec<T> vec) {
		this.vec = Objects.requireNonNull(vec);
	}

	@Override
	public boolean contains(Object arg0) {
		return vec.contains(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return vec.containsAll(arg0);
	}

	@Override
	public T get(int arg0) {
		return vec.get(arg0);
	}

	@Override
	public int indexOf(Object arg0) {
		return vec.indexOf(arg0);
	}

	@Override
	public boolean isEmpty() {
		return vec.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return vec.iterator();
	}

	@Override
	public int lastIndexOf(Object arg0) {
		return vec.lastIndexOf(arg0);
	}

	@Override
	public ListIterator<T> listIterator() {
		return null;
	}

	@Override
	public ListIterator<T> listIterator(int arg0) {
		return null;
	}

	@Override
	public int size() {
		return vec.size;
	}

	@Override
	public List<T> subList(int arg0, int arg1) {
		return null;
	}

	@Override
	public Object[] toArray() {
		return vec.toArray();
	}

	@Override
	public <E> E[] toArray(E[] arg0) {
		return vec.toArray(arg0);
	}

	@Override
	public Spliterator<T> spliterator() {
		return vec.spliterator();
	}

	@Override
	public Stream<T> parallelStream() {
		return vec.parallelStream();
	}

	@Override
	public Stream<T> stream() {
		return vec.stream();
	}

	@Override
	public void forEach(Consumer<? super T> arg0) {
		vec.forEach(arg0);
	}

	@Override
	public boolean equals(Object arg0) {
		return vec.equals(arg0);
	}

	@Override
	public int hashCode() {
		return vec.hashCode();
	}

	@Override
	public String toString() {
		return vec.toString();
	}

	@Override
	public T[] toArray(IntFunction<T[]> generator) {
		return vec.toArray(generator);
	}
}
