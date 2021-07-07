package remotetask;

public abstract class CancelableRemoteProxy extends RemoteProxy {
	private volatile boolean canceled = false;
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	@Override
	public void cancel() {
		canceled = true;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}
}
