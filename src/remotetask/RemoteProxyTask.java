package remotetask;

import remotetask.pointcloud.task_pointcloud;
import remotetask.pointdb.task_pointdb;
import remotetask.rasterdb.task_rasterdb;
import remotetask.vectordb.task_vectordb;

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
		remoteProxy.setMessageProxy(this);
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
		if(clazz.isAnnotationPresent(task_rasterdb.class)) {
			return "rasterdb/" + clazz.getAnnotation(task_rasterdb.class).value();
		}
		if(clazz.isAnnotationPresent(task_pointdb.class)) {
			return "pointdb/" + clazz.getAnnotation(task_pointdb.class).value();
		}		
		if(clazz.isAnnotationPresent(task_pointcloud.class)) {
			return "pointcloud/" + clazz.getAnnotation(task_pointcloud.class).value();
		}
		if(clazz.isAnnotationPresent(task_vectordb.class)) {
			return "vectordb/" + clazz.getAnnotation(task_vectordb.class).value();
		}
		RemoteProxy r = remoteProxy;
		if(r != null) {
			return r.getName();
		}
		return clazz.getSimpleName();
	}
}
