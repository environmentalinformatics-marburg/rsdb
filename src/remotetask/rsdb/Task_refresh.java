package remotetask.rsdb;

import broker.Broker;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;

@task_rsdb("refresh")
@Description("Refresh RSDB layers list on the server. After this you need to refresh (or reload) the Web interface to be up to date.")
public class Task_refresh extends CancelableRemoteTask {
	private int throw_error_rounds = -1;

	public Task_refresh(Context ctx) {
		super(ctx);
		this.throw_error_rounds = ctx.task.optInt("throw_error_rounds", -1);		
	}

	@Override
	protected void process() throws Exception {
		setMessage("start refresh");
		Broker broker = ctx.broker;
		broker.refreshPoiGroupMap();
		broker.refreshPointcloudConfigs();
		broker.refreshRasterdbConfigs();
		broker.refreshRoiGroupMap();
		broker.refreshVectordbConfigs();
		broker.refreshVoxeldbConfigs();
		setMessage("finish refresh");
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
