package util.property;

import java.util.function.IntUnaryOperator;

public class IntMapBind extends AbstractIntWatchable {
	
	public final IntWatchable watchable;
	public final IntUnaryOperator mapper;

	public IntMapBind(IntWatchable watchable, IntUnaryOperator mapper) {
		this.watchable = watchable;
		this.mapper = mapper;
		IntWatcher mapWatcher = (o,v)->{
			if(watchers!=null) {
				for(IntWatcher watcher:watchers) {
					watcher.changed(this, mapper.applyAsInt(v));
				}
			}
		};
		watchable.watch(mapWatcher);
	}

	@Override
	public int get() {
		return mapper.applyAsInt(watchable.get());
	}
}
