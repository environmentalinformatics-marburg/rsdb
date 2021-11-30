package remotetask.rasterdb;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.Band;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import util.JsonUtil;
import util.Range2d;
import util.tiff.file.TiffFile.TiffCompression;
import util.tiff.file.TiledWriter;
import util.tiff.file.TiledWriter.BandType;

@task_rasterdb("export")
@Description("Export raster pixel data to GeoTIFF file.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer. (source)", example="rasterdb1")
@Param(name="rect", type="number_rect", desc="Extent to export. Default: Full raster layer area will be exported.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="bands", type="integer_array", desc="Array of integer band numbers. Default: all bands.", example="1, 2, 3", required=false)
@Param(name="data_type", desc="Pixel data type (same data type for all bands).\n Types: INT16, FLOAT32.\n If left empty, smallest type fitting all bands with no loss in precision will be chosen.", format="INT16 or FLOAT32", example="FLOAT32", required=false)
@Param(name="compression", desc="Type of TIFF compression. Types:\n NO: no compression (fast, high space demand),\n DEFLATE: zip-like compression (compatibility with many applications),\n ZSTD: Zstandard compression (fast, low storage demands, compatibility verified with recent GDAL based applications like Qgis).\n Default: NO", format="NO or DEFLATE or ZSTD", example="ZSTD", required=false)
public class Task_export extends CancelableRemoteProxyTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB rasterdb;
	private Range2d range = null;
	private int[] bandIndices = null;
	private BandType bandType = null;	
	private TiffCompression tiffCompression;

	public Task_export(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		rasterdb = broker.getRasterdb(name);
		rasterdb.check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);


		JSONArray rect_Text = task.optJSONArray("rect");
		if(rect_Text != null) {
			double rect_xmin = rect_Text.getDouble(0);
			double rect_ymin = rect_Text.getDouble(1);
			double rect_xmax = rect_Text.getDouble(2);
			double rect_ymax = rect_Text.getDouble(3);	
			range = rasterdb.ref().bboxToRange2d(rect_xmin, rect_ymin, rect_xmax, rect_ymax);
		}

		if(task.has("bands")) {
			this.bandIndices = JsonUtil.getIntArray(ctx.task, "bands");
		}
		String dataTypeText = task.optString("data_type");
		this.bandType = BandType.parse(dataTypeText, null);
		String compressionText = task.optString("compression");
		this.tiffCompression = TiffCompression.parse(compressionText, TiffCompression.NO);
	}

	@Override
	public void process() throws Exception {
		setMessage("prepare");
		//GeoReference ref = rasterdb.ref();
		//Range2d range = ref.bboxToRange2d(new double[] {512920, 5353805, 544770, 5376220});
		if(range == null) {
			range = rasterdb.getLocalRange(false);
		}
		Band[] bands;
		if(bandIndices == null || bandIndices.length == 0) {
			bands = rasterdb.bandMapReadonly.values().toArray(Band[]::new);
		} else {
			bands = Arrays.stream(bandIndices).mapToObj(bandIndex -> rasterdb.getBandByNumberThrow(bandIndex)).toArray(Band[]::new);			
		}
		String filename = "temp/testingTiff.tif";
		//int timestamp = 60598080;
		//int timestamp = 0;
		int timestamp = rasterdb.rasterUnit().timeKeysReadonly().last();
		TiledWriter tiledWriter = new TiledWriter(rasterdb, bandType, tiffCompression, range, bands, timestamp, filename);
		setMessage("export");
		setRemoteProxyAndRunAndClose(tiledWriter);
		setMessage("done");
	}
}
