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

public class ReadonlyArray<E> implements ReadonlyList<E>, RandomAccess, Serializable, Cloneable {
	private static final long serialVersionUID = -125263339894434798L;

	private final E[] a;	

	public ReadonlyArray(E[] array) {
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
		E[] a = this.a;
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
	public Iterator<E> iterator() {
		return new ReadonlyArrayIterator<E>(a);
	}

	@Override
	public Object[] toArray() {
		return a.clone();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] target) { //derived from ArrayList
		int size = a.length;
		if (target.length < size) {
			return (T[]) Arrays.copyOf(a, size, target.getClass());
		}
		System.arraycopy(a, 0, target, 0, size);
		if (target.length > size) {
			target[size] = null;
		}
		return target;
	}
	
	@Override
	public E[] toArray(IntFunction<E[]> generator) {
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
		ReadonlyArray<E> other = (ReadonlyArray<E>) obj;
		if (!Arrays.equals(a, other.a))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(a);
	}

	@Override
	public void forEach(Consumer<? super E> action) {		
		for(E e:a) {
			action.accept(e);
		}
	}

	@Override
	public Spliterator<E> spliterator() {
		return Arrays.spliterator(a);
	}

	@Override
	public Stream<E> stream() {
		return Arrays.stream(a);
	}

	@Override
	public String toString() {
		return Arrays.toString(a);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new ReadonlyArray<E>(a.clone());
	}

	@Override
	public E get(int index) {
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
	public ListIterator<E> listIterator() {
		return new ReadonlyArrayIterator<E>(a);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new ReadonlyArrayIterator<E>(a, index, a.length);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new ReadonlySubArray<E>(a, fromIndex, toIndex);
	}
}
