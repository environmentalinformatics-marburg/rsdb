package remotetask.pointdb;

import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointdb.PointDB;
import pointdb.base.Tile;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_pointdb("verify")
@Description("Check point data.")
@Param(name="pointdb", type="pointdb", desc="ID of PointDB layer.", example="pointdb1")
public class Task_verify extends CancelableRemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointDB pointdb;

	public Task_verify(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointdb");
		pointdb = broker.getPointdb(name);
		pointdb.config.getAcl().check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() {
		setMessage("query count of tiles");
		long total = pointdb.tileMetaMap.sizeLong();
		setMessage("start processing " + total + " tiles");
		TileProducer tileProducer = pointdb.tileProducer(null);


		Consumer consumer = new Consumer(total, tileProducer);
		tileProducer.produce(consumer);
		if(consumer.getCnt() == total) {
		setMessage("all " + total + " tiles processed");
		} else {
			throw new RuntimeException("stopped (" + consumer.cnt + " of " + total + " tiles processed)");
		}
	}
	
	private final class Consumer implements TileConsumer {
		private final long total;
		private final TileProducer tileProducer;
		private LongAdder cnt = new LongAdder();

		private Consumer(long total, TileProducer tileProducer) {
			this.total = total;
			this.tileProducer = tileProducer;
		}

		@Override
		public void nextTile(Tile tile) {
			cnt.increment();
			if(isMessageTime()) {
				setMessage(getCnt() + " of " + total + " tiles processed");
				if(isCanceled()) {
					log.info("tileProducer.requestStop");
					tileProducer.requestStop();
				}
			}
		}
		public long getCnt() {
			return cnt.sum();
		}
	}
}
