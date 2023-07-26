package util.collections.array;

import java.io.Serializable;
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
import java.util.stream.Stream;

import util.collections.ReadonlyList;
import util.collections.array.iterator.ReadonlyArrayIterator;
import util.collections.vec.IndexedConsumer;
import util.collections.vec.IndexedThrowableConsumer;

public class ReadonlyArray<T> implements ReadonlyList<T>, RandomAccess, Serializable, Cloneable {
	private static final long serialVersionUID = -125263339894434798L;

	private final T[] a;	

	public ReadonlyArray(T[] array) {
		a = Objects.requireNonNull(array);
	}

	@Override
	public int size() {
		return a.length;
	}

	@Override
	public boolean isEmpty() {
		return a.length == 0;
	}

	@Override
	public boolean contains(Object o) {
		T[] a = this.a;
		if (o == null) {
			for (int i = 0; i < a.length; i++)
				if (a[i] == null)
					return true;
		} else {
			for (int i = 0; i < a.length; i++)
				if (o.equals(a[i]))
					return true;
		}
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return new ReadonlyArrayIterator<T>(a);
	}

	@Override
	public Object[] toArray() {
		return a.clone();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E[] toArray(E[] target) { //derived from ArrayList
		int size = a.length;
		if (target.length < size) {
			return (E[]) Arrays.copyOf(a, size, target.getClass());
		}
		System.arraycopy(a, 0, target, 0, size);
		if (target.length > size) {
			target[size] = null;
		}
		return target;
	}
	
	@Override
	public <E> E[] toArray(IntFunction<E[]> generator){
		int len = a.length;
		E[] r = generator.apply(len);
		System.arraycopy(a, 0, r, 0, len);
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		ReadonlyArray<T> other = (ReadonlyArray<T>) obj;
		if (!Arrays.equals(a, other.a))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(a);
	}

	@Override
	public void forEach(Consumer<? super T> action) {		
		for(T e:a) {
			action.accept(e);
		}
	}
	
	@Override
	public void forEachIndexed(IndexedConsumer<? super T> consumer) {
		T[] data = a;
		int len = data.length;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i], i);
		}
	}
	
	@Override
	public <E extends Exception> void forEachIndexedThrowable(IndexedThrowableConsumer<? super T, E> consumer) throws E {
		T[] data = a;
		int len = data.length;
		for (int i = 0; i < len; i++) {
			consumer.accept(data[i], i);
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		return Arrays.spliterator(a);
	}

	@Override
	public Stream<T> stream() {
		return Arrays.stream(a);
	}

	@Override
	public String toString() {
		return Arrays.toString(a);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new ReadonlyArray<T>(a.clone());
	}

	@Override
	public T get(int index) {
		return a[index];
	}

	@Override
	public int indexOf(Object o) { //derived from ArrayList
		int size = a.length;
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (a[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(a[i]))
                    return i;
        }
        return -1;
    }

	@Override
	 public int lastIndexOf(Object o) { //derived from ArrayList
		int size = a.length;
		if (o == null) {
            for (int i = size-1; i >= 0; i--)
                if (a[i]==null)
                    return i;
        } else {
            for (int i = size-1; i >= 0; i--)
                if (o.equals(a[i]))
                    return i;
        }
        return -1;
    }

	@Override
	public ListIterator<T> listIterator() {
		return new ReadonlyArrayIterator<T>(a);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return new ReadonlyArrayIterator<T>(a, index, a.length);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return new ReadonlySubArray<T>(a, fromIndex, toIndex);
	}
	
	
}
