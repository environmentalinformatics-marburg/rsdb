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
import util.collections.array.iterator.ReadonlyArrayIterator;
import util.collections.vec.IndexedConsumer;
import util.collections.vec.IndexedThrowableConsumer;

public class ReadonlySizedArray<T> implements ReadonlyList<T>, RandomAccess, Serializable, Cloneable {
	private static final long serialVersionUID = -373989095388709654L;

	private final T[] a;
	private final int size;

	public ReadonlySizedArray(T[] array, int size) {
		a = Objects.requireNonNull(array);
		if(size < 0 || size > a.length) {
			throw new IndexOutOfBoundsException(""+size);
		}
		this.size= size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean contains(Object o) {
		T[] a = this.a;
		if (o == null) {
			for (int i = 0; i < size; i++)
				if (a[i] == null)
					return true;
		} else {
			for (int i = 0; i < size; i++)
				if (o.equals(a[i]))
					return true;
		}
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return new ReadonlyArrayIterator<T>(a, 0, size);
	}

	@Override
	public Object[] toArray() {
		@SuppressWarnings("unchecked")
		T[] r = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		System.arraycopy(a, 0, r, 0, size);
		return r;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E[] toArray(E[] target) {
		if (target.length < size) {
			E[] r = (E[]) Array.newInstance(target.getClass().getComponentType(), size);
			System.arraycopy(a, 0, r, 0, size);
			return r;
		}
		System.arraycopy(a, 0, target, 0, size);
		if (target.length > size) {
			target[size] = null;
		}
		return target;
	}

	@Override
	public <E> E[] toArray(IntFunction<E[]> generator) {
		E[] r = generator.apply(size);
		System.arraycopy(a, 0, r, 0, size);
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
		for (int i = 0; i < size; i++) {
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
		ReadonlySizedArray<?> other = (ReadonlySizedArray<?>) obj;
		if (!Arrays.equals(a, other.a))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	@Override
	public void forEach(Consumer<? super T> action) {		
		for (int i = 0; i < size; i++) {
			action.accept(a[i]);
		}
	}
	
	@Override
	public void forEachIndexed(IndexedConsumer<? super T> consumer) {
		int len = size;
		T[] data = a;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i], i);
		}
	}
	
	@Override
	public <E extends Exception> void forEachIndexedThrowable(IndexedThrowableConsumer<? super T, E> consumer) throws E {
		int len = size;
		T[] data = a;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i], i);
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		return Arrays.spliterator(a, 0, size);
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public Stream<T> stream() {
		return Arrays.stream(a, 0, size);
	}

	@Override
	public String toString() {//dervied from Arrays
		if (a == null)
            return "null";

        int iMax = size - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(String.valueOf(a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new ReadonlySizedArray<T>(a.clone(),size);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException("readonly");
	}

	@Override
	public T get(int index) {
		if(index >= size) {
			throw new IndexOutOfBoundsException(""+index);
		}
		return a[index];
	}

	@Override
	public int indexOf(Object o) { //derived from ArrayList
		if (o == null) {
			for (int i = 0; i < size; i++) {
				if (a[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (o.equals(a[i]))
					return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) { //derived from ArrayList
		if (o == null) {
			for (int i = size - 1; i >= 0; i--)
				if (a[i]==null)
					return i;
		} else {
			for (int i = size - 1; i >= 0; i--)
				if (o.equals(a[i]))
					return i;
		}
		return -1;
	}

	@Override
	public ListIterator<T> listIterator() {
		return new ReadonlyArrayIterator<T>(a, 0, size);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return new ReadonlyArrayIterator<T>(a, index, size);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex > this.size) {
			throw new IndexOutOfBoundsException(""+fromIndex+" "+toIndex);
		}
		return new ReadonlySubArray<T>(a, fromIndex, toIndex);
	}
}
