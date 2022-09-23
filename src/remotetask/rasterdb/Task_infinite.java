package remotetask.rasterdb;

import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;

@task_rasterdb("infinite")
@Description("This task does nothing. It is for testing purposes. The running task can be canceled.")
public class Task_infinite extends CancelableRemoteTask {
	//private final Broker broker;
	//private final JSONObject task;

	public Task_infinite(Context ctx) {
		super(ctx);
		//this.broker = ctx.broker;
		//this.task = ctx.task;
	}

	@Override
	protected void process() throws Exception {
		long cnt = 0;
		
		while(true) {
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
			cnt++;
			setMessage("round " + cnt);			
			Thread.sleep(100);
		}		
	}
}
