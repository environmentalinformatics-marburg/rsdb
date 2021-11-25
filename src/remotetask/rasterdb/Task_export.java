package remotetask.rasterdb;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import util.Range2d;
import util.tiff.file.TiledWriter;
import util.tiff.file.TiffFile.TiffCompression;
import util.tiff.file.TiledWriter.BandType;

@task_rasterdb("export")
@Description("Export raster pixel data to GeoTIFF file.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer. (source)", example="rasterdb1")
public class Task_export extends CancelableRemoteProxyTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB rasterdb;

	public Task_export(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		rasterdb = broker.getRasterdb(name);
		rasterdb.check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);		
	}

	@Override
	public void process() throws Exception {
		setMessage("prepare");
		//GeoReference ref = rasterdb.ref();
		//Range2d range = ref.bboxToRange2d(new double[] {512920, 5353805, 544770, 5376220});
		Range2d range = rasterdb.getLocalRange(false);
		String filename = "temp/testingTiff.tif";
		//int timestamp = 60598080;
		//int timestamp = 0;
		int timestamp = rasterdb.rasterUnit().timeKeysReadonly().last();
		TiledWriter tiledWriter = new TiledWriter(rasterdb, BandType.FLOAT32, TiffCompression.NO, range, timestamp, filename);
		setMessage("export");
		setRemoteProxyAndRunAndClose(tiledWriter);
		setMessage("done");
	}
}
