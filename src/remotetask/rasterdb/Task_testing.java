package remotetask.rasterdb;

import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_rasterdb("testing")
@Description("This task does nothing. It is for testing purposes. The running task can be canceled. Optionally, this task throws an error at round x.")
@Param(name="throw_error_rounds", type="integer", desc="Throw error after x rounds. Default -1 => do not throw an error", example="0", required=false)
public class Task_testing extends CancelableRemoteTask {
	private int throw_error_rounds = -1;

	public Task_testing(Context ctx) {
		super(ctx);
		this.throw_error_rounds = ctx.task.optInt("throw_error_rounds", -1);		
	}

	@Override
	protected void process() throws Exception {
		long cnt = 0;
		
		while(true) {
			checkForRoundError(cnt);
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
			cnt++;
			setMessage("round " + cnt);			
			Thread.sleep(100);
		}		
	}
	
	private void checkForRoundError(long cnt) {
		if(cnt == throw_error_rounds) {
			throwRoundError();
		}
	}
	
	public void throwRoundError() {
		throw new RuntimeException("error round reached");
	}
}
