package util.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import util.collections.array.ReadonlyArray;
import util.collections.vec.IndexedConsumer;
import util.collections.vec.IndexedThrowableConsumer;
import util.collections.vec.Vec;

public interface ReadonlyList<T> extends List<T> {
	
	@SuppressWarnings("rawtypes")
	public static final ReadonlyArray EMPTY = new ReadonlyArray<>(new Object[0]);
	
	public static <E> ReadonlyArray<E> of(E... array) {
		if(array == null || array.length == 0) {
			return EMPTY;
		}
		return new ReadonlyArray<E>(array);
	}
	
	@Override
	public default boolean add(T e) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default boolean remove(Object o) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default boolean addAll(Collection<? extends T> c) {
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
	public default T set(int index, T element) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public default void add(int index, T element) {
		throw new UnsupportedOperationException("readonly");		
	}

	@Override
	public default T remove(int index) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	public default boolean removeIf(Predicate<? super T> filter) {
		throw new UnsupportedOperationException("readonly");
	}
	
	@Override
	default void replaceAll(UnaryOperator<T> operator) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	default void sort(Comparator<? super T> c) {
		throw new UnsupportedOperationException("readonly");
	}
	
	public <E> E[] toArray(IntFunction<E[]> generator);
	
	public default <E> E[] mapArray(Function<T, E> mapper) {
		@SuppressWarnings("unchecked")
		E[] a = (E[]) new Object[size()];
		int i = 0;
		for(T e : this) {
			E v = mapper.apply(e);
			a[i] = v;
		}
		return a;
	}
	
	public default <E> E[] mapArray(IntFunction<E[]> generator, Function<T, E> mapper) {
		E[] a = generator.apply(size());
		int i = 0;
		for(T e : this) {
			E v = mapper.apply(e);
			a[i] = v;
		}
		return a;
	}
	
	public default T first() {
		return this.get(0);
	}
	
	public default T last() {
		return get(size() - 1);
	}
	
	default public Vec<T> copyVec() {
		@SuppressWarnings("unchecked")
		T[] elements = (T[]) toArray();
		return new Vec<T>(elements);		
	}
	
	public void forEachIndexed(IndexedConsumer<? super T> consumer);
	
	public <E extends Exception> void forEachIndexedThrowable(IndexedThrowableConsumer<? super T, E> consumer) throws E;
}
