package util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import util.collections.iterator.ReadonlyIteratorAdapter;

public class ReadonlyNavigableSetView<T> implements NavigableSet<T> {
	
	private final NavigableSet<T> set;
	
	public ReadonlyNavigableSetView(NavigableSet<T> set) {
		this.set = set;
	}

	@Override
	public Comparator<? super T> comparator() {
		return set.comparator();
	}

	@Override
	public T first() {
		return set.first();
	}

	@Override
	public T last() {
		return set.last();
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public <E> E[] toArray(E[] a) {
		return set.toArray(a);
	}

	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();		
	}

	@Override
	public T lower(T e) {
		return set.lower(e);
	}

	@Override
	public T floor(T e) {
		return set.floor(e);
	}

	@Override
	public T ceiling(T e) {
		return set.ceiling(e);
	}

	@Override
	public T higher(T e) {
		return set.higher(e);
	}

	@Override
	public T pollFirst() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T pollLast() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		return new ReadonlyIteratorAdapter<T>(set.iterator());
	}

	@Override
	public NavigableSet<T> descendingSet() {
		return new ReadonlyNavigableSetView<T>(set.descendingSet());
	}

	@Override
	public Iterator<T> descendingIterator() {
		return new ReadonlyIteratorAdapter<T>(set.descendingIterator());
	}

	@Override
	public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {		
		return new ReadonlyNavigableSetView<T>(set.subSet(fromElement, fromInclusive, toElement, toInclusive));
	}

	@Override
	public NavigableSet<T> headSet(T toElement, boolean inclusive) {
		return new ReadonlyNavigableSetView<T>(set.headSet(toElement, inclusive));
	}

	@Override
	public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
		return new ReadonlyNavigableSetView<T>(set.tailSet(fromElement, inclusive));
	}

	@Override
	public SortedSet<T> subSet(T fromElement, T toElement) {
		return Collections.unmodifiableSortedSet(set.subSet(fromElement, toElement));
	}

	@Override
	public SortedSet<T> headSet(T toElement) {
		return Collections.unmodifiableSortedSet(set.headSet(toElement));
	}

	@Override
	public SortedSet<T> tailSet(T fromElement) {
		return Collections.unmodifiableSortedSet(set.tailSet(fromElement));
	}

	@Override
	public Spliterator<T> spliterator() {
		return set.spliterator();
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Stream<T> stream() {
		return set.stream();
	}

	@Override
	public Stream<T> parallelStream() {
		return set.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		set.forEach(action);
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return set.equals(obj);
	}

	@Override
	public String toString() {
		return set.toString();
	}
}
