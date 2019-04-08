package util.property;

public class IntBindValue extends AbstractIntWatchable implements IntSettable {

	private int v;

	private IntWatchable binding;
	private IntWatcher bindingWatcher;

	public IntBindValue(int v) {
		this.v = v;
		this.binding = null;
		this.bindingWatcher = null;
	}

	@Override
	public int get() {
		return v;
	}

	private void setDirect(int v) {
		if(this.v!=v) {
			this.v = v;
			if(watchers!=null) {
				for(IntWatcher watcher:watchers) {
					watcher.changed(this, v);
				}
			}
		}
	}

	@Override
	public void set(int v) {
		if(binding==null) {
			setDirect(v);
		} else {
			if(binding instanceof IntSettable) {
				((IntSettable) binding).set(v);
			} else {
				throw new RuntimeException("value bound "+binding.getClass());
			}
		}
	}

	public void bind(IntWatchable binding) {
		unbind();
		this.binding = binding;
		this.bindingWatcher = (bind_o, bind_v) -> setDirect(bind_v);
		binding.watch(bindingWatcher);
		setDirect(binding.get());
	}
	
	public void bindRead(IntWatchable binding) {
		if(binding instanceof IntSettable) {
			bind(new IntWatchableWrapper(binding));
		} else {
			bind(binding);
		}
	}

	public void unbind() {
		if(binding!=null) {
			binding.unwatch(bindingWatcher);
			binding = null;
			bindingWatcher = null;
		}
	}
}
