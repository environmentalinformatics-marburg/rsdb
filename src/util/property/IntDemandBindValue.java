package util.property;

public class IntDemandBindValue extends IntValue {
	
	private IntWatchable binding;
	private IntWatcher bindingWatcher;

	public IntDemandBindValue(int v) {
		super(v);
		this.binding = null;
		this.bindingWatcher = null;
	}

	@Override
	public int get() {
		if(binding==null) {
			return v;
		} else {
			return binding.get();
		}
	}

	@Override
	public void set(int v) {
		if(binding==null) {
			super.set(v);
		} else {
			throw new RuntimeException("value bound");
		}
	}
	
	public void bind(IntWatchable binding) {
		unbind();
		this.binding = binding;
		this.bindingWatcher = (bind_o, bind_v)->{
			if(watchers!=null) {
				for(IntWatcher watcher:watchers) {
					watcher.changed(this, bind_v);
				}
			}
		};
		binding.watch(bindingWatcher);
	}
	
	public void unbind() {
		if(binding!=null) {
			binding.unwatch(bindingWatcher);
			binding = null;
			bindingWatcher = null;
		}
	}
}
