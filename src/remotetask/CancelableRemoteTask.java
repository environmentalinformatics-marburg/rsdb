package remotetask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class CancelableRemoteTask extends RemoteTask {
	private static final Logger log = LogManager.getLogger();
	
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
