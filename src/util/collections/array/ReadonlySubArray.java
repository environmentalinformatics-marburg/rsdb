package util.collections.array;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import util.collections.ReadonlyList;
import util.collections.array.iterator.ReadonlySubArrayIterator;
import util.collections.vec.IndexedConsumer;
import util.collections.vec.IndexedThrowableConsumer;

public class ReadonlySubArray<T> implements ReadonlyList<T>, RandomAccess, Serializable, Cloneable {
	private static final long serialVersionUID = -373989095388709654L;

	private final T[] a;
	private final int fromIndex;
	private final int toIndex;

	public ReadonlySubArray(T[] array, int fromIndex, int toIndex) {
		a = Objects.requireNonNull(array);
		if(fromIndex < 0 || toIndex < fromIndex || toIndex > a.length) {
			throw new IndexOutOfBoundsException(""+fromIndex+"  "+toIndex);
		}
		this.fromIndex = fromIndex;
		this.toIndex= toIndex;
	}
	
	@Override
	public int size() {
		return toIndex-fromIndex;
	}

	@Override
	public boolean isEmpty() {
		return toIndex-fromIndex == 0;
	}

	@Override
	public boolean contains(Object o) {
		T[] a = this.a;
		if (o == null) {
			for (int i = fromIndex; i < toIndex; i++)
				if (a[i] == null)
					return true;
		} else {
			for (int i = fromIndex; i < toIndex; i++)
				if (o.equals(a[i]))
					return true;
		}
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return new ReadonlySubArrayIterator<T>(a,fromIndex,toIndex);
	}

	@Override
	public Object[] toArray() {
		int size = toIndex-fromIndex;
		@SuppressWarnings("unchecked")
		T[] r = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		System.arraycopy(a, fromIndex, r, 0, size);
		return r;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E[] toArray(E[] target) {
		int size = toIndex - fromIndex;
		if (target.length < size) {
			E[] r = (E[]) Array.newInstance(target.getClass().getComponentType(), size);
			System.arraycopy(a, fromIndex, r, 0, size);
			return r;
		}
		System.arraycopy(a, fromIndex, target, 0, size);
		if (target.length > size) {
			target[size] = null;
		}
		return target;
	}
	
	@Override
	public <E> E[] toArray(IntFunction<E[]> generator) {
		int len = toIndex - fromIndex;
		E[] r = generator.apply(len);
		System.arraycopy(a, fromIndex, r, 0, len);
		return r;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() { // derived from Arrays
		int result = 1;
		for (int i = fromIndex; i < toIndex; i++) {
			result = 31 * result + (a[i] == null ? 0 : a[i].hashCode());
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) { //no element by element check
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadonlySubArray<?> other = (ReadonlySubArray<?>) obj;
		if (!Arrays.equals(a, other.a))
			return false;
		if (fromIndex != other.fromIndex)
			return false;
		if (toIndex != other.toIndex)
			return false;
		return true;
	}

	@Override
	public void forEach(Consumer<? super T> action) {		
		for (int i = fromIndex; i < toIndex; i++) {
			action.accept(a[i]);
		}
	}
	
	public void forEachIndexed(IndexedConsumer<? super T> consumer) {
		for (int i = fromIndex; i < toIndex; i++) {
			consumer.accept(a[i], i - fromIndex);
		}
	}
	
	public <E extends Exception> void forEachIndexedThrowable(IndexedThrowableConsumer<? super T, E> consumer) throws E {
		for (int i = fromIndex; i < toIndex; i++) {
			consumer.accept(a[i], i - fromIndex);
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		return Arrays.spliterator(a,fromIndex,toIndex);
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public Stream<T> stream() {
		return Arrays.stream(a, fromIndex, toIndex);
	}

	@Override
	public String toString() {//dervied from Arrays
		if (a == null)
			return "null";

		int iMax = toIndex - 1;
		if (iMax-fromIndex == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = fromIndex; ; i++) {
			b.append(String.valueOf(a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new ReadonlySubArray<T>(a.clone(),fromIndex,toIndex);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public T get(int index) {
		if(index>=toIndex) {
			throw new IndexOutOfBoundsException(""+index);
		}
		return a[index - fromIndex];
	}

	@Override
	public int indexOf(Object o) { //derived from ArrayList
		if (o == null) {
			for (int i = fromIndex; i < toIndex; i++) {
				if (a[i]==null) {
					return i;
				}
			}
		} else {
			for (int i = fromIndex; i < toIndex; i++) {
				if (o.equals(a[i]))
					return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) { //derived from ArrayList
		if (o == null) {
			for (int i = toIndex - 1; i >= fromIndex; i--)
				if (a[i]==null)
					return i;
		} else {
			for (int i = toIndex - 1; i >= fromIndex; i--)
				if (o.equals(a[i]))
					return i;
		}
		return -1;
	}

	@Override
	public ListIterator<T> listIterator() {
		return new ReadonlySubArrayIterator<T>(a, fromIndex, toIndex);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return new ReadonlySubArrayIterator<T>(a, fromIndex, fromIndex+index,toIndex);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		if(fromIndex<0 || this.fromIndex+toIndex>this.toIndex) {
			throw new IndexOutOfBoundsException(""+fromIndex+" "+toIndex);
		}
		return new ReadonlySubArray<T>(a, this.fromIndex+fromIndex, this.fromIndex+toIndex);
	}
}
