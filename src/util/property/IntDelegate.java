package util.property;

public abstract class IntDelegate extends AbstractIntWatchable implements IntSettable {
	protected void setChanged(int v) {
		if(watchers!=null) {
			for(IntWatcher watcher:watchers) {
				watcher.changed(this, v);
			}
		}
	}
}
