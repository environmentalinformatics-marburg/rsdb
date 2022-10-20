package util.collections;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.function.Consumer;

public class CountingCollection<T> extends AbstractCollection<T> {
	
	public int count = 0;
	
	public static class CountingIterator<T> implements Iterator<T> {
		
		int count; 
		
		public CountingIterator(int count) {
			this.count = count;
		}

		@Override
		public boolean hasNext() {
			return count > 0;
		}

		@Override
		public T next() {
			count--;
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {			
			for(; count > 0; count--) {
				action.accept(null);
			}
		}	
	}

	@Override
	public Iterator<T> iterator() {
		return new CountingIterator<>(count);
	}

	@Override
	public int size() {
		return count;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	@Override
	public boolean add(T e) {
		count++;
		return true;
	}
	
	public void push(T e) {
		count++;
	}

	@Override
	public void clear() {
		count = 0;
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		for (int i = 0; i < count; i++) {
			action.accept(null);
		}
	}
}
