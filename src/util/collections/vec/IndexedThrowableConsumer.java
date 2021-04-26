package util.collections.vec;

public interface IndexedThrowableConsumer<T, E extends Exception> {
	void accept(T t, int i) throws E;
}