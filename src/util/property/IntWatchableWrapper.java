package util.property;

public class IntWatchableWrapper implements IntWatchable {
	
	private final IntWatchable w;
	
	public IntWatchableWrapper(IntWatchable w) {
		this.w = w;
	}

	@Override
	public int get() {
		return w.get();
	}

	@Override
	public void watch(IntWatcher watcher) {
		w.watch(watcher);		
	}

	@Override
	public void unwatch(IntWatcher watcher) {
		w.unwatch(watcher);		
	}

}
