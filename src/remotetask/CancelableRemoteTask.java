package remotetask;

import util.CarpException;

public abstract class CancelableRemoteTask extends RemoteTask  {	
	private volatile boolean canceled = false;
	
	public CancelableRemoteTask(Context ctx) {
		super(ctx);
	}

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
	
	public void throwCanceled() {
		if(canceled) {
			throw new CarpException("canceled");
		}
	}
}
