package remotetask.pointcloud;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.PointCloud;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_pointcloud("verify")
@Description("Check point data.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointDB layer.", example="pointcloud1")
public class Task_verify extends CancelableRemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud pointcloud;

	public Task_verify(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		pointcloud = broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() {
		setMessage("query count of tiles");		
		long total = pointcloud.getTileKeys().size();
		setMessage("start processing " + total + " tiles");
		Stream<CellTable> cellTables = pointcloud.getCellTables(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, new AttributeSelector().all());

		StreamConsumer consumer = new StreamConsumer(total);
		cellTables.forEach(consumer);
		if(consumer.getCnt() == total) {
			setMessage("all " + total + " tiles processed");
		} else {
			throw new RuntimeException("stopped (" + consumer.cnt + " of " + total + " tiles processed)");
		}
	}

	private final class StreamConsumer implements Consumer<CellTable> {
		private final long total;
		private LongAdder cnt = new LongAdder();
		@Override
		public void accept(CellTable t) {
			cnt.increment();
			if(isMessageTime()) {
				setMessage(getCnt() + " of " + total + " tiles processed");
				if(isCanceled()) {
					throw new RuntimeException("canceled");
				}
			}			
		}
		public StreamConsumer(long total) {
			this.total = total;
		}
		public long getCnt() {
			return cnt.sum();
		}
	}
}
