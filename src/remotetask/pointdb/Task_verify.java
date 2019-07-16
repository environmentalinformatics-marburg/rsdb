package remotetask.pointdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointdb.PointDB;
import pointdb.base.Tile;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_pointdb("verify")
@Description("check point data")
@Param(name="pointdb", desc="ID of PointDB layer")
public class Task_verify extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointDB pointdb;

	public Task_verify(Context ctx) {
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


		TileConsumer consumer = new TileConsumer() {
			long cnt;
			@Override
			public void nextTile(Tile tile) {
				cnt++;
				if(isMessageTime()) {
					setMessage(cnt + " of " + total + " tiles processed");
				}
			}
		};
		tileProducer.produce(consumer);
		setMessage("all " + total + " tiles processed");	
	}
}
