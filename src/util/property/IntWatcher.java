package util.property;

@FunctionalInterface
public interface IntWatcher {
	public void changed(Object o, int v);
}