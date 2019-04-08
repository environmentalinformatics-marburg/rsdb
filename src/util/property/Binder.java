package util.property;

public class Binder {
	public static IntWatchable sum(IntWatchable a, IntWatchable b) {
		return new AbstractIntWatchable() {
			
			private final IntWatcher sourceWatcher = (bind_o, bind_v) -> {
				if(watchers!=null) {
					int v = get();
					for(IntWatcher watcher:watchers) {
						watcher.changed(this, v);
					}
				}
			};
			
			{
				a.watch(sourceWatcher);
				b.watch(sourceWatcher);
			}
			
			@Override
			public int get() {
				return a.get()+b.get();
			}
		};
	}
}
