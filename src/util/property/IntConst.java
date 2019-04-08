package util.property;

public class IntConst implements IntWatchable {
	
	public final int v;
	
	public IntConst(int v) {
		this.v = v;
	}

	@Override
	public int get() {
		return v;
	}

	@Override
	public void watch(IntWatcher watcher) {}

	@Override
	public void unwatch(IntWatcher watcher) {}

}
