package remotetask;

import org.tinylog.Logger;

public class CancelableRemoteProxyTask extends RemoteProxyTask {	

	public CancelableRemoteProxyTask(Context ctx) {
		super(ctx);
	}	

	@Override
	public boolean isCancelable() {
		RemoteProxy r = this.remoteProxy;
		if(r == null) {
			//throw new RuntimeException("missing RemoteProxy");
			Logger.warn("missing RemoteProxy for " + this.getClass());
			return true;
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
			//throw new RuntimeException("missing RemoteProxy");
			Logger.warn("missing RemoteProxy for " + this.getClass());
			return false;
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
	
	public void setRemoteProxyAndRunAndClose(CancelableRemoteProxy remoteProxy) throws Exception {
		super.setRemoteProxyAndRunAndClose(remoteProxy);
	}
	
	@Override
	public void setRemoteProxyAndRunAndClose(RemoteProxy remoteProxy) throws Exception {
		if(!(remoteProxy instanceof CancelableRemoteProxy)) {
			throw new RuntimeException("remoteProxy needs to be of type CancelableRemoteProxy");
		}
		setRemoteProxyAndRunAndClose((CancelableRemoteProxy) remoteProxy);		
	}
	
	
}
