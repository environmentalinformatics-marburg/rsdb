package remotetask;

public class RemoteProxyTask extends RemoteTask {

	protected RemoteProxy remoteProxy = null;

	public RemoteProxyTask(Context ctx) {
		super(ctx);
	}

	public static RemoteProxyTask of(RemoteProxy remoteProxy, Context ctx) {
		RemoteProxyTask remoteProxyTask = new RemoteProxyTask(ctx);
		remoteProxyTask.setRemoteProxy(remoteProxy);
		return remoteProxyTask;
	}

	public void setRemoteProxy(RemoteProxy remoteProxy) {
		this.remoteProxy = remoteProxy;
		remoteProxy.setMessageSink(this);
	}

	public void setRemoteProxyAndRunAndClose(RemoteProxy remoteProxy) throws Exception {
		setRemoteProxy(remoteProxy);
		remoteProxy.process();
		remoteProxy.close();
	}

	@Override
	protected void process() throws Exception {
		RemoteProxy r = remoteProxy;
		if(r != null) {
			r.process();
		}
	}

	@Override
	protected void close() {
		RemoteProxy r = remoteProxy;
		if(r != null) {
			r.close();
		}
	}

	public String getName() {		
		Class<? extends RemoteTask> clazz = this.getClass();
		String tname = getTaskName(clazz);
		if(tname == null) {
			RemoteProxy r = remoteProxy;
			if(r != null) {
				return r.getName();
			}
			return clazz.getSimpleName();
		} else {
			return tname;
		}
	}
}
