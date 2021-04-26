package util.collections.vec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.Collector.Characteristics;

import util.collections.ReadonlyList;
import util.collections.array.ReadonlyArray;
import util.collections.array.ReadonlySizedArray;
import util.collections.array.iterator.ReadonlyArrayIterator;
import util.collections.array.iterator.ReadonlyArrayReverseIterator;
import util.collections.vec.Vec.VecCollector;
import util.collections.vec.iterator.VecListIterator;

public class Vec<T> implements List<T> {
	//private static final Logger log = LogManager.getLogger();

	private static final Object[] DEFAULT_SIZED_EMPTY_ARRAY = {};

	protected int size;
	protected T[] items;

	@SuppressWarnings("unchecked")
	public static <T> Vec<T> ofOne(T e) {
		return new Vec<T>((T[]) new Object[] {e}, 1);
	}
	
	public static <T> Vec<T> of(Collection<T> collection) {
		@SuppressWarnings("unchecked")
		T[] items = (T[]) collection.toArray();
		return new Vec<T>(items);
	}

	@SuppressWarnings("unchecked")
	public Vec() {
		items = (T[]) DEFAULT_SIZED_EMPTY_ARRAY;
	}

	@SuppressWarnings("unchecked")
	public Vec(int initialCapacity) {
		items = (T[]) new Object[initialCapacity];
	}

	public Vec(T[] items) {
		this.items = items;
		this.size = items.length;
	}

	public Vec(T[] items, int size) {
		this.items = items;
		this.size = size;
	}

	/**
	 * high performance readonly iterator
	 */
	@Override
	public ReadonlyArrayIterator<T> iterator() {
		return new ReadonlyArrayIterator<T>(items, 0, size);
	}
	
	public Iterator<T> reverseIterator() {
		return new ReadonlyArrayReverseIterator<T>(items, size, size);
	}
	
	public void reverse() {
		T[] data = items;
		for (int start = 0, end = size - 1; start <= end; start++, end--) {
		    T temp = data[start];
		    data[start] = data[end];
		    data[end] = temp;
		}
	}
	
	public Iterable<T> asReverseIterable() {
		return new ReverseIterableVecView<T>(this);
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i]);
		}
	}
	
	public void forEachIndexed(IndexedConsumer<? super T> consumer) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i], i);
		}
	}
	
	public <E extends Exception> void forEachIndexedThrowable(IndexedThrowableConsumer<? super T, E> consumer) throws E {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i], i);
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		return Arrays.spliterator(items, 0, size);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(items, size);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E[] toArray(E[] a) {
		if (a.length < size) {
			return (E[]) Arrays.copyOf(items, size, a.getClass());
		}
		System.arraycopy(items, 0, a, 0, size);
		if (a.length > size) {
			a[size] = null;
		}
		return a;
	}
	
	public <E> E[] toArray(IntFunction<E[]> generator) {
		int len = size;
		E[] a = generator.apply(len);
		System.arraycopy(items, 0, a, 0, len);
		return a;
	}
	
	public <E> E[] toArray(IntFunction<E[]> generator, int fromIndex, int toIndex) {
		int len = toIndex - fromIndex;
		E[] a = generator.apply(len);
		System.arraycopy(items, fromIndex, a, 0, len);
		return a;
	}

	@Override
	public void clear() {		
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			data[i] = null;
		}		
		size = 0;
	}

	public void clearNoCleanup() {		
		size = 0;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@SuppressWarnings("unchecked")
	private void growForOne() {
		if (items == DEFAULT_SIZED_EMPTY_ARRAY) {
			items = (T[]) new Object[10];
		} else {
			int oldLen = size;
			int newLen = oldLen + (oldLen >> 1) + 1;
			T[] newItems = (T[]) new Object[newLen];
			System.arraycopy(this.items, 0, newItems, 0, oldLen);
			items = newItems;
		}
	}

	@Override
	public boolean add(T e) {		
		if (items.length == size) {
			growForOne();
		}
		items[size++] = e;
		return true;
	}
	
	public boolean addIfNotContained(T e) {
		if(contains(e)) {
			return false;
		}
		return add(e);
	}

	public void addFast(T e) {		
		if (items.length == size) {
			growForOne();
		}
		items[size++] = e;
	}

	public void addNoGrow(T e) {		
		items[size++] = e;
	}

	private void grow(int minSize) {
		int len = size;
		int minGrow = len + (len >> 1) + 1;
		int growSize =  minSize <= minGrow ? minGrow : minSize;
		@SuppressWarnings("unchecked")
		T[] newItems = (T[]) new Object[growSize];
		System.arraycopy(items, 0, newItems, 0, len);
		items = newItems;
	}


	@Override
	public boolean addAll(Collection<? extends T> c) {
		@SuppressWarnings("unchecked")
		T[] a = (T[]) c.toArray();
		int addLen = a.length;
		int newSize = size + addLen;
		if(items.length < newSize) {
			grow(newSize);
		}
		System.arraycopy(a, 0, items, size, addLen);
		size = newSize;
		return true;
	}

	public void addAllFast(Collection<? extends T> c) {
		@SuppressWarnings("unchecked")
		T[] a = (T[]) c.toArray();
		int addLen = a.length;
		int newSize = size + addLen;
		if(items.length < newSize) {
			grow(newSize);
		}
		System.arraycopy(a, 0, items, size, addLen);
		size = newSize;
	}

	public void addAll(Vec<? extends T> vec) {
		T[] a = vec.items;
		int addLen = vec.size;
		int newSize = size + addLen;
		if(items.length < newSize) {
			grow(newSize);
		}
		System.arraycopy(a, 0, items, size, addLen);
		size = newSize;
	}

	public void addNoGrow(Collection<? extends T> c) {
		@SuppressWarnings("unchecked")
		T[] a = (T[]) c.toArray();
		int addLen = a.length;
		int newSize = size + addLen;
		System.arraycopy(a, 0, items, size, addLen);
		size = newSize;
	}

	@Override
	public T get(int index) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}		
		return this.items[index];       
	}
	
	public Optional<T> optGet(int index) {
		if(this.size <= index || index < 0) {
			return Optional.empty();
		}		
		return Optional.of(this.items[index]);       
	}

	public T getNoCheck(int index) {
		return this.items[index];       
	}

	@Override
	public void sort(Comparator<? super T> c) {
		Arrays.sort(items, 0, size, c);
	}
	
	public void parallelSort(Comparator<? super T> c) {
		Arrays.parallelSort(items, 0, size, c);
	}

	public T[] items() {
		return items;
	}

	public Vec<T> copy() {
		T[] elementData = Arrays.copyOf(items, size);
		return new Vec<T>(elementData);		
	}
	
	public ReadonlyArray<T> copyReadonly() {
		T[] elementData = Arrays.copyOf(items, size);
		return new ReadonlyArray<T>(elementData);		
	}

	/**
	 * All changes in vec are visible in view.
	 * @return
	 */
	public ReadonlyVecView<T> readonlyView() {
		return new ReadonlyVecView<T>(this);
	}
	
	/**
	 * Changes in vec may or may not be visibale in view.
	 * 
	 * May be smaller and more performant than readonlyView.
	 * @return
	 */
	public ReadonlySizedArray<T> readonlyWeakView() {
		return new ReadonlySizedArray<T>(items, size);
	}

	public T first() {
		if(size == 0) {
			throw new IndexOutOfBoundsException();
		}	
		return items[0];
	}

	/**
	 * get last element
	 * @return
	 */
	public T last() {
		return items[size - 1];
	}
	
	/**
	 * Get element of lastIndex starting with last element.
	 * lastIndex == 0  ==>  last element
	 * lastIndex == 1  ==>  previous of last element
	 * @param lastIndex
	 * @return
	 */
	public T getFromLast(int lastIndex) {
		return items[size - 1 - lastIndex];
	}
	
	/**
	 * Get element of index if index is zero or positive.
	 * Get element starting with last index if index is negative.
	 * signedIndex == 0 ==> first element
	 * signedIndex == 1 ==> second element
	 * signedIndex == -1 ==> last element
	 * signedIndex == -2 ==> previous of last element
	 * @param signedIndex
	 * @return
	 */
	public T getSignedNoCheck(int signedIndex) {
		return signedIndex < 0 ? items[size + signedIndex] : items[signedIndex];
	}
	
	/**
	 * Get element of index if index is zero or positive.
	 * Get element starting with last index if index is negative.
	 * signedIndex == 0 ==> first element
	 * signedIndex == 1 ==> second element
	 * signedIndex == -1 ==> last element
	 * signedIndex == -2 ==> previous of last element
	 * @param signedIndex
	 * @return
	 */
	public T getSigned(int signedIndex) {
		int index = signedIndex < 0 ? size + signedIndex : signedIndex;
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}		
		return this.items[index];
	}

	public <R> R[] mapArray(Function<? super T, R> mapper) {
		int len = size;
		T[] data = items;
		@SuppressWarnings("unchecked")
		R[] result = (R[]) new Object[len];
		for (int i = 0; i < len; i++) {
			result[i] = mapper.apply(data[i]);
		}
		return result;
	}

	public <R> Vec<R> map(Function<? super T, R> mapper) {
		return new Vec<R>(mapArray(mapper));
	}
	
	public <R> R[] mapArray(Function<? super T, R> mapper, IntFunction<R[]> generator) {
		int len = size;
		T[] data = items;
		R[] result = generator.apply(len);
		for (int i = 0; i < len; i++) {
			result[i] = mapper.apply(data[i]);
		}
		return result;
	}

	public double[] mapDoubleArray(ToDoubleFunction<T> mapper) {
		int len = size;
		T[] data = items;
		double[] result = new double[len];
		for (int i = 0; i < len; i++) {
			result[i] = mapper.applyAsDouble(data[i]);
		}
		return result;
	}

	public DoubleVec mapDouble(ToDoubleFunction<T> mapper) {
		return new DoubleVec(mapDoubleArray(mapper));
	}

	public int count(Predicate<? super T> predicate) {
		int len = size;
		T[] data = items;
		int cnt = 0;
		for (int i = 0; i < len; i++) {
			if(predicate.test(data[i])) {
				cnt++;
			}
		}
		return cnt;
	}

	public Vec<T> filter(Predicate<? super T> predicate) {
		int len = size;
		T[] data = items;
		Vec<T> result = new Vec<T>();
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(predicate.test(e)) {
				result.add(e);
			}
		}
		return result;
	}

	@Override
	public ReadonlyVecSubView<T> subList(int fromIndex, int toIndex) {
		return new ReadonlyVecSubView<T>(this, fromIndex, toIndex);
	}

	@Override
	public void replaceAll(UnaryOperator<T> operator) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			data[i] = operator.apply(data[i]);
		}
	}

	@Override
	public boolean contains(Object o) {
		int len = size;
		T[] data = items;
		if (o==null) {
			for (int i = 0; i < len; i++) {
				if(data[i] == null) {
					return true;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				if(o.equals(data[i])) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int indexOf(Object o) {
		int len = size;
		T[] data = items;
		if (o==null) {
			for (int i = 0; i < len; i++) {
				if(data[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				if(o.equals(data[i])) {
					return i;
				}
			}
		}
		return -1;
	}

	public boolean equals(Vec<T> vec) {
		if(this == vec) {
			return true;
		}
		if(vec == null) {
			return false;
		}
		int len1 = size;
		int len2 = vec.size;
		if(len1 != len2) {
			return false;
		}
		T[] data1 = items;
		T[] data2 = vec.items;		
		for (int i = 0; i < len1; i++) {
			T v1 = data1[i];
			T v2 = data2[i];
			if(v1 != v2 && (v1 == null || !v1.equals(v2))) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if(o instanceof Vec) {
			return equals((Vec<T>) o);
		}
		if (!(o instanceof List)) {
			return false;
		}
		ListIterator<T> e1 = listIterator();
		ListIterator<?> e2 = ((List<?>) o).listIterator();
		while (e1.hasNext() && e2.hasNext()) {
			T o1 = e1.next();
			Object o2 = e2.next();
			if (!(o1==null ? o2==null : o1.equals(o2)))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	@Override
	public int hashCode() {
		int len = size;
		T[] data = items;
		int hashCode = 1;        
		for (int i = 0; i < len; i++) {
			T e = data[i];
			hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
		}
		return hashCode;
	}

	@Override
	public String toString() {
		int lenM1 = size - 1;
		if(lenM1 == -1) {
			return "[]";
		}
		T[] data = items;
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0;; i++) {
			T e = data[i];
			sb.append(e == this ? "(self)" : e);
			if(i == lenM1) {
				return sb.append(']').toString();
			}
			sb.append(',').append(' ');
		}
	}

	@Override
	public T remove(int index) {
		int len = size;
		T[] data = items;
		if(len <= index) {
			throw new IndexOutOfBoundsException();
		}		
		T ret = data[index];   
		int movStart = index + 1;
		if(movStart < len) {
			System.arraycopy(data, movStart, data, index, len - movStart);
		}
		data[--size] = null;
		return ret;
	}

	public void removeFast(int index) {
		int len = size;
		T[] data = items;
		if(len <= index) {
			throw new IndexOutOfBoundsException();
		}		
		int movStart = index + 1;
		if(movStart < len) {
			System.arraycopy(data, movStart, data, index, len - movStart);
		}
		data[--size] = null;
	}

	@Override
	public Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public Stream<T> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		for (Object e : collection) {
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean remove(Object e) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			if(e.equals(data[i])) {
				int movStart = i + 1;
				if(movStart < len) {
					System.arraycopy(data, movStart, data, i, len - movStart);
				}
				data[--size] = null;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean removeIf(Predicate<? super T> predicate) {
		int len = size;
		T[] data = items;
		int pos = 0;
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(!predicate.test(e)) {
				if(pos != i) {
					data[pos] = e;
				}
				pos++;
			}
		}
		if(pos < len) {
			size = pos;
			for(; pos < len; pos++) {
				data[pos] = null;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		int len = size;
		T[] data = items;
		int pos = 0;
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(!c.contains(e)) {
				if(pos != i) {
					data[pos] = e;
				}
				pos++;
			}
		}
		if(pos < len) {
			size = pos;
			for(; pos < len; pos++) {
				data[pos] = null;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		int len = size;
		T[] data = items;
		int pos = 0;
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(c.contains(e)) {
				if(pos != i) {
					data[pos] = e;
				}
				pos++;
			}
		}
		if(pos < len) {
			size = pos;
			for(; pos < len; pos++) {
				data[pos] = null;
			}
			return true;
		}
		return false;
	}

	public boolean retainIf(Predicate<? super T> predicate) {
		int len = size;
		T[] data = items;
		int pos = 0;
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(predicate.test(e)) {
				if(pos != i) {
					data[pos] = e;
				}
				pos++;
			}
		}
		if(pos < len) {
			size = pos;
			for(; pos < len; pos++) {
				data[pos] = null;
			}
			return true;
		}
		return false;
	}

	@Override
	public T set(int index, T e) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}
		T old = items[index];
		items[index] = e;
		return old;
	}

	public void setFast(int index, T e) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}
		items[index] = e;
	}

	@Override
	public int lastIndexOf(Object o) {
		T[] data = items;
		if (o==null) {
			for (int i = size - 1; i >= 0; i--) {
				if(data[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = size - 1; i >= 0; i--) {
				if(o.equals(data[i])) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public void add(int index, T e) {
		int len = size;
		if(len <= index) {
			throw new IndexOutOfBoundsException();
		}		
		if (items.length == len) {
			growForOne();
		}
		T[] data = items;
		System.arraycopy(data, index, data, index + 1, len - index);
		data[index] = e;
		size++;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		return addAll(index, (T[]) c.toArray());
	}

	public void ensureCapacity(int minCapacity) {
		if(items.length < minCapacity) {
			grow(minCapacity);
		}
	}

	public boolean addAll(int index, T[] a) {
		int aLen = a.length;
		if(aLen == 0) {
			return false;
		}
		int len = size;
		if(len <= index) {
			throw new IndexOutOfBoundsException();
		}
		int newLen = len + aLen;
		ensureCapacity(newLen);
		int movedLen = len - index;
		if(movedLen > 0) {
			System.arraycopy(items, index, items, index + aLen, movedLen);
		}
		System.arraycopy(a, 0, items, index, aLen);
		this.size = newLen;
		return true;
	}
	
	public int lastIndex() {
		return size - 1;
	}

	/**
	 * high performance readonly iterator
	 */
	@Override
	public VecListIterator<T> listIterator() {
		return new VecListIterator<T>(this);
	}

	/**
	 * high performance readonly iterator
	 */
	@Override
	public VecListIterator<T> listIterator(int index) {
		return new VecListIterator<T>(this, index);
	}
	
	/**
	 * elements can be removed from vec
	 * @return
	 */
	public VecListIterator<T> mutableListIterator() {
		return new VecListIterator<T>(this);
	}
	
	/**
	 * elements can be removed from vec
	 * @return
	 */
	public VecListIterator<T> mutableListIterator(int index) {
		return new VecListIterator<T>(this, index);
	}
	
	public int elementSum(ToIntFunction<? super T> fun) {
		int len = size;
		T[] data = items;
		int sum = 0;
		for (int i = 0; i < len; i++) {			
			sum += fun.applyAsInt(data[i]);
		}
		return sum;
	}
	
	public static class VecCollector<E> implements Collector<E, Vec<E>, Vec<E>> {
		
		private static final EnumSet<Characteristics> CH_ID = EnumSet.of(Collector.Characteristics.IDENTITY_FINISH);
		
		private VecCollector() {}

		@Override
		public Supplier<Vec<E>> supplier() {
			return Vec::new;
		}

		@Override
		public BiConsumer<Vec<E>, E> accumulator() {
			return Vec::addFast;
		}

		@Override
		public BinaryOperator<Vec<E>> combiner() {
			return (left, right) -> { left.addAll(right); return left; };
		}

		@Override
		public Function<Vec<E>, Vec<E>> finisher() {
			return lds -> lds;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return CH_ID;
		}
	}
	
	public static final VecCollector<?> COLLECTOR = new VecCollector<>();
	
	@SuppressWarnings("unchecked")
	public static <E> VecCollector<E> collector() {
		return (VecCollector<E>) COLLECTOR;
	}
}
