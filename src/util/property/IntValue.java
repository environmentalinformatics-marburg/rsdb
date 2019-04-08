package util.property;

public class IntValue extends AbstractIntWatchable implements IntSettable {

	protected int v;

	public IntValue(int v) {
		this.v = v;
	}

	@Override
	public int get() {
		return v;
	}

	@Override
	public void set(int v) {
		if(this.v!=v) {
			this.v = v;
			if(watchers!=null) {
				for(IntWatcher watcher:watchers) {
					watcher.changed(this, v);
				}
			}
		}
	}
}
