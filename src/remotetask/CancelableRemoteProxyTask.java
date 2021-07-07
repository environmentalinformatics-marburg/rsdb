package remotetask;

public class CancelableRemoteProxyTask extends RemoteProxyTask {

	public CancelableRemoteProxyTask(Context ctx) {
		super(ctx);
	}
	
	

	@Override
	public boolean isCancelable() {
		RemoteProxy r = this.remoteProxy;
		if(r == null) {
			throw new RuntimeException("missing RemoteProxy");
		}
		return r.isCancelable();
	}

	@Override
	public void cancel() {
		RemoteProxy r = this.remoteProxy;
		if(r == null) {
			throw new RuntimeException("missing RemoteProxy");
		}
		r.cancel();
	}

	@Override
	public boolean isCanceled() {
		RemoteProxy r = this.remoteProxy;
		if(r == null) {
			throw new RuntimeException("missing RemoteProxy");
		}
		return r.isCanceled();
	}

	public void setRemoteProxy(CancelableRemoteProxy remoteProxy) {
		super.setRemoteProxy(remoteProxy);
	}

	@Override
	public void setRemoteProxy(RemoteProxy remoteProxy) {
		if(!(remoteProxy instanceof CancelableRemoteProxy)) {
			throw new RuntimeException("remoteProxy needs to be of type CancelableRemoteProxy");
		}
		setRemoteProxy((CancelableRemoteProxy) remoteProxy);
	}	
}
