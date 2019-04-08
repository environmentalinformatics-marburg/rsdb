package util.property;

public abstract class AbstractIntWatchable implements IntWatchable {
	
	protected IntWatcher[] watchers;
	
	public AbstractIntWatchable() {
		watchers = null;
	}

	@Override
	public void watch(IntWatcher watcher) {
		if(watchers==null) {
			watchers = new IntWatcher[]{watcher};
		} else {
			int len = watchers.length;
			IntWatcher[] ws = new IntWatcher[len+1];
			for (int i = 0; i < len; i++) {
				ws[i] = watchers[i];
			}
			ws[len] = watcher;
			watchers = ws;
		}
	}

	@Override
	public void unwatch(IntWatcher watcher) {
		if(watchers!=null) {
			int index = -1;
			int len = watchers.length;
			for (int i = 0; i < len; i++) {
				if(watchers[i]==watcher) {
					index = i;
					break;
				}
			}
			if(index>=0) {
				if(len==1) {
					watchers = null;
				} else {
					IntWatcher[] ws = new IntWatcher[len-1];
					int j = 0;
					for (int i = 0; i < len; i++) {
						if(i!=index) {
							ws[j++] = watchers[i];
						}
					}
					watchers = ws;
				}
			}
		}
	}

}
