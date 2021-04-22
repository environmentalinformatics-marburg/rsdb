package util.collections.vec;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SyncVec<T> implements Iterable<T> {
	//private static final Logger log = LogManager.getLogger();

	private static final Object[] DEFAULT_SIZED_EMPTY_ARRAY = {};

	protected final ReentrantReadWriteLock lock;
	protected int size;
	protected T[] items;	

	public static <T> Vec<T> ofOne(T e) {
		return new Vec<T>((T[]) new Object[] {e}, 1);
	}

	public SyncVec() {
		this.lock = new ReentrantReadWriteLock();
		this.items = (T[]) DEFAULT_SIZED_EMPTY_ARRAY;
	}

	public SyncVec(ReentrantReadWriteLock lock) {		
		this.lock = lock;
		this.items = (T[]) DEFAULT_SIZED_EMPTY_ARRAY;
	}

	public SyncVec(int initialCapacity) {
		this.lock = new ReentrantReadWriteLock();
		this.items = (T[]) new Object[initialCapacity];
	}

	public SyncVec(ReentrantReadWriteLock lock, int initialCapacity) {
		this.lock = lock;
		this.items = (T[]) new Object[initialCapacity];
	}

	public SyncVec(T[] items) {
		this.lock = new ReentrantReadWriteLock();
		this.items = items;
		this.size = items.length;
	}

	public SyncVec(ReentrantReadWriteLock lock, T[] items) {
		this.lock = lock;
		this.items = items;
		this.size = items.length;
	}

	public SyncVec(T[] items, int size) {
		lock = new ReentrantReadWriteLock();
		this.items = items;
		this.size = size;
	}

	public SyncVec(ReentrantReadWriteLock lock, T[] items, int size) {
		this.lock = lock;
		this.items = items;
		this.size = size;
	}

	public ReentrantReadWriteLock getLock() {
		return lock;
	}

	public ReadLock getReadLock() {
		return lock.readLock();
	}

	public WriteLock getWriteLock() {
		return lock.writeLock();
	}

	@Override
	public Iterator<T> iterator() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		lock.readLock().lock();
		try {
			forEachUnsync(action);
		} finally {
			lock.readLock().unlock();
		}
	}

	public void forEachUnsync(Consumer<? super T> action) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			action.accept(data[i]);
		}
	}
	
	@FunctionalInterface
	public static interface IndexedConsumer<T> {
		void accept(int i, final T t);
	}
	
	public void forEachIndexedUnsync(IndexedConsumer<? super T> action) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			action.accept(i, data[i]);
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		throw new RuntimeException("not implemented");
	}

	public void addUnsync(T e) {	
		if (items.length == size) {
			growForOneUnsync();
		}
		items[size++] = e;
	}		

	private void growForOneUnsync() {
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
	
	public T get(int index) {
		lock.readLock().lock();
		try {
			return getUnsync(index);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public T getUnsync(int index) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}		
		return this.items[index];       
	}

	public void lockRead() {
		lock.readLock().lock();		
	}
	
	public void unlockRead() {
		lock.readLock().unlock();		
	}
	
	public void lockWrite() {
		lock.writeLock().lock();		
	}
	
	public void unlockWrite() {
		lock.writeLock().unlock();		
	}
	
	public int findIndexOf(Predicate<? super T> predicate) {
		lock.readLock().lock();
		try {
			return findIndexOfUnsync(predicate);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public int findIndexOfUnsync(Predicate<? super T> predicate) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(predicate.test(e)) {
				return i;
			}
		}		
		return -1;
	}
	
	public T find(Predicate<? super T> predicate) {
		lock.readLock().lock();
		try {
			return findUnsync(predicate);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public T findUnsync(Predicate<? super T> predicate) {
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			T e = data[i];
			if(predicate.test(e)) {
				return e;
			}
		}		
		return null;
	}
	
	public void setUnsync(int index, T e) {
		if(this.size <= index) {
			throw new IndexOutOfBoundsException();
		}
		items[index] = e;
	}
	
	public void clearUnsync() {		
		int len = size;
		T[] data = items;
		for (int i = 0; i < len; i++) {
			data[i] = null;
		}		
		size = 0;
	}

	public void sortUnsync(Comparator<? super T> c) {
		Arrays.sort(items, 0, size, c);
	}
}
