package util.collections.vec.iterator;

import java.util.ListIterator;

import util.collections.vec.Vec;

public class Vec2ListIterator<E> implements ListIterator<E> {
	
	private final Vec<E> vec;
	private int pos;
	
	public Vec2ListIterator(Vec<E> vec) {
		this.vec = vec;
		this.pos = 0;
	}

	@Override
	public boolean hasNext() {
		return pos < vec.size();
	}

	@Override
	public E next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPrevious() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public E previous() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int nextIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int previousIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub		
	}

	@Override
	public void set(E e) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void add(E e) {
		// TODO Auto-generated method stub		
	}
}
