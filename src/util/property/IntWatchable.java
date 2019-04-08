package util.property;

public interface IntWatchable {	
	public int get();
	public void watch(IntWatcher watcher);
	public void unwatch(IntWatcher watcher);
}
