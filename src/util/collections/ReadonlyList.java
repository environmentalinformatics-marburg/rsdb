package util.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import util.collections.array.ReadonlyArray;

public interface ReadonlyList<E> extends List<E> {
	
	@SuppressWarnings("rawtypes")
	public static final ReadonlyArray EMPTY = new ReadonlyArray<>(new Object[0]);
	
	public static <E> ReadonlyArray<E> of(E... array) {
		if(array == null || array.length == 0) {
			return EMPTY;
		}
		return new ReadonlyArray<E>(array);
	}
	
	@Override
	public default boolean add(E e) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default boolean remove(Object o) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default void clear() {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default E set(int index, E element) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default void add(int index, E element) {
		throw new UnsupportedOperationException("readonly");		
	}

	@Override
	public default E remove(int index) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	default void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	default void sort(Comparator<? super E> c) {
		throw new UnsupportedOperationException("readonly");
	}
	
	public E[] toArray(IntFunction<E[]> generator);
	
	public default E first() {
		return this.get(0);
	}
	
	public default E last() {
		return get(size() - 1);
	}
}
