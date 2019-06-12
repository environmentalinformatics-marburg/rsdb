package remotetask.pointdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointdb.PointDB;
import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.process.DataProvider2;
import pointdb.process.Functions;
import pointdb.process.ProcessingFun;
import pointdb.subsetdsl.Region;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.ProcessingFloat;
import rasterdb.RasterDB;
import rasterdb.TilePixel;
import rasterdb.TimeBandProcessor;
import rasterunit.RasterUnit;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.BlokingTaskSubmitter;
import util.BlokingTaskSubmitter.PhasedTask;
import util.Range2d;
import util.Timer;
import util.frame.BooleanFrame;

@task_pointdb("index_raster")
@Description("create raster of pointcloud with index metrics calculations. Existing target RasterDB layer is needed.")
@Param(name="pointdb", desc="ID of PointDB layer (source)")
@Param(name="rasterdb", type="rasterdb", desc="existing ID of RasterDB layer (target)")
@Param(name="indices",  desc="list of indices")
@Param(name="rect",  desc="extent to process")
@Param(name="mask_band",  desc="band number of mask in RasterDB layer", required=false)
public class Task_index_raster extends RemoteTask{
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointDB pointdb;

	public Task_index_raster(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointdb");
		pointdb = broker.getPointdb(name);
		pointdb.config.getAcl().check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() {
		String rasterdb_name = task.getString("rasterdb");
		RasterDB rasterdb = broker.getRasterdb(rasterdb_name);		
		JSONArray indices_text = task.getJSONArray("indices");
		Band maskBand = null;
		if(task.has("mask_band")) {
			int maskBandIndex = task.getInt("mask_band");
			maskBand = rasterdb.getBandByNumber(maskBandIndex);
		}

		int indices_len = indices_text.length();
		ProcessingFun[] indices = new ProcessingFun[indices_len];
		Band[] bands = new Band[indices_len];
		for (int i = 0; i < indices_len; i++) {
			String index_text = indices_text.getString(i);
			indices[i] = Functions.getFun(index_text);
			bands[i] = rasterdb.createBand(TilePixel.TYPE_FLOAT, index_text, null);
		}


		JSONArray rect_Text = task.getJSONArray("rect");
		double xmin = rect_Text.getDouble(0);
		double ymin = rect_Text.getDouble(1);
		double xmax = rect_Text.getDouble(2);
		double ymax = rect_Text.getDouble(3);

		log.info("query rect "+xmin+" "+ymin+" "+xmax+" "+ymax);

		GeoReference ref = rasterdb.ref();
		int raster_xmin = ref.geoXToPixel(xmin);
		int raster_ymin = ref.geoYToPixel(ymin);
		int raster_xmax = ref.geoXToPixel(xmax);
		int raster_ymax = ref.geoYToPixel(ymax);

		log.info("raster rect "+raster_xmin+" "+raster_ymin+" "+raster_xmax+" "+raster_ymax);

		Timer.start("index_raster processing");
		if(maskBand != null) {
			log.info("use mask band");
		} else {
			log.info("use NO mask band");	
		}
		long processed_pixel = process_rect(pointdb, rasterdb, ref, raster_xmin, raster_ymin, raster_xmax, raster_ymax, bands, indices, maskBand);		
		Timer timer = Timer.stop("index_raster processing");
		long xsize = raster_xmax - raster_xmin + 1;
		long ysize = raster_ymax - raster_ymin + 1;
		long pixel = xsize * ysize;
		log.info(timer+"   size " + xsize + " x " + ysize+":  " + processed_pixel + " pixel  of rect pixel " + pixel + "  " +  (((double)(timer.end - timer.begin))/(processed_pixel)) + " ms / pixel");


		rasterdb.rebuildPyramid();
	}

	private static long MAX_RASTER_BYTES = 268_435_456;

	public long process_rect(PointDB pointdb, RasterDB rasterdb, GeoReference ref, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, Band[] bands, ProcessingFun[] indices, Band maskBand) {
		long processed_pixel = 0;
		long xsize = raster_xmax - raster_xmin + 1;
		long ysize = raster_ymax - raster_ymin + 1;
		long maxPixel = (MAX_RASTER_BYTES / 4) / indices.length;
		long pixel = xsize * ysize;
		if(pixel <= maxPixel || (xsize <= 1 && ysize <= 1)) {
			log.info("process rect "+raster_xmin+" "+raster_ymin+" "+raster_xmax+" "+raster_ymax+"     size " + xsize + " x " + ysize + "  =  " + pixel+" pixel");

			boolean[][] mask = null;
			if(maskBand != null) {
				Range2d range2d = new Range2d(raster_xmin, raster_ymin, raster_xmax, raster_ymax);
				int timestamp = 0;
				TimeBandProcessor processor = new TimeBandProcessor(rasterdb, range2d);
				BooleanFrame maskFrame = processor.getFloatFrame(timestamp, maskBand).toMask();
				mask = maskFrame.data;
			}
			processed_pixel += run_rect(pointdb, rasterdb.rasterUnit(), ref, raster_xmin, raster_ymin, raster_xmax, raster_ymax, bands, indices, mask);			
		} else if(xsize >= ysize) {
			int raster_xmid = (int) ((((long)raster_xmin) + ((long)raster_xmax)) >> 1);
			log.info("divide x " + raster_xmin + "  " + raster_xmid + "  " + raster_xmax);
			processed_pixel += process_rect(pointdb, rasterdb, ref, raster_xmin, raster_ymin, raster_xmid, raster_ymax, bands, indices, maskBand);
			processed_pixel += process_rect(pointdb, rasterdb, ref, raster_xmid + 1, raster_ymin, raster_xmax, raster_ymax, bands, indices, maskBand);
		} else {
			int raster_ymid = (int) ((((long)raster_ymin) + ((long)raster_ymax)) >> 1);
			log.info("divide y " + raster_ymin + "  " + raster_ymid + "  " + raster_ymax);
			processed_pixel += process_rect(pointdb, rasterdb, ref, raster_xmin, raster_ymin, raster_xmax, raster_ymid, bands, indices, maskBand);
			processed_pixel += process_rect(pointdb, rasterdb, ref, raster_xmin, raster_ymid + 1, raster_xmax, raster_ymax, bands, indices, maskBand);
		}
		return processed_pixel;
	}

	public long run_rect(PointDB pointdb, RasterUnit rasterUnit, GeoReference ref, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, Band[] bands, ProcessingFun[] indices, boolean[][] mask) {
		BlokingTaskSubmitter blokingTaskSubmitter = new BlokingTaskSubmitter();

		int indices_len = indices.length;
		int width = raster_xmax - raster_xmin + 1;
		int height = raster_ymax - raster_ymin + 1;
		float[][][] pixels = new float[indices_len][height][width];
		for (int i = 0; i < indices_len; i++) {
			float[][] ind = pixels[i];
			for (int y = 0; y < height; y++) {
				float[] row = ind[y];
				for (int x = 0; x < width; x++) {
					row[x] = Float.NaN;
				}
			}
		}
		Timer.start("index_raster part processing");
		long processed_pixel = 0;
		for (int y = 0; y < height; y++) {
			Timer.start("line");
			for (int x = 0; x < width; x++) {
				if(mask == null || mask[y][x]) {
					double xgeo = ref.pixelXToGeo(raster_xmin + x);
					double ygeo = ref.pixelYToGeo(raster_ymin + y);
					double xgeo1 = ref.pixelXToGeo(raster_xmin + x + 1);
					double ygeo1 = ref.pixelYToGeo(raster_ymin +y + 1);
					long pxmin = PdbConst.to_utmm(xgeo);
					long pymin = PdbConst.to_utmm(ygeo);
					long pxmax = PdbConst.to_utmm(xgeo1) - 1;
					long pymax = PdbConst.to_utmm(ygeo1) - 1;
					Rect pRect = Rect.of_UTMM(pxmin, pymin, pxmax, pymax);
					PixelTask pixeltask = new PixelTask(blokingTaskSubmitter, pointdb, pRect, indices, pixels, x, y);
					blokingTaskSubmitter.submit(pixeltask);
					processed_pixel++;
				}
			}
			log.info(Timer.stop("line") + "     " + (y+1) +" of "+height);
		}
		blokingTaskSubmitter.finish();
		Timer timer = Timer.stop("index_raster part processing");
		log.info(timer+"   size " + width + " x " + height+":  " + processed_pixel + " pixel  of rect pixel " + (width*height) + "  " +  (((double)(timer.end - timer.begin))/(processed_pixel)) + " ms / pixel");

		log.info("raster write "+raster_xmin + " " + raster_ymin + "      " + ref.pixelXToGeo(raster_xmin) + " " + ref.pixelYToGeo(raster_ymin)  );
		for (int i = 0; i < indices_len; i++) {
			ProcessingFloat.writeMerge(rasterUnit, 0, bands[i], pixels[i], raster_ymin, raster_xmin);
		}
		rasterUnit.commit();
		return processed_pixel;
	}

	private static class PixelTask extends PhasedTask {

		private final PointDB pointdb;
		private final Rect pRect;
		private final ProcessingFun[] indices;
		private final float[][][] pixels;
		private final int x;
		private final int y;

		public PixelTask(BlokingTaskSubmitter blokingTaskSubmitter, PointDB pointdb, Rect pRect, ProcessingFun[] indices, float[][][] pixels, int x, int y) {
			super(blokingTaskSubmitter);
			this.pointdb = pointdb;
			this.pRect = pRect;
			this.indices = indices;
			this.pixels = pixels;
			this.x = x;
			this.y = y;
		}

		@Override
		public void run() {
			DataProvider2 dp = new DataProvider2(pointdb, Region.ofRect(pRect));
			int indices_len = indices.length;
			for (int i = 0; i < indices_len; i++) {
				try {						
					pixels[i][y][x] = (float) indices[i].process(dp);
				} catch(Exception e) {
					//e.printStackTrace();					
					log.warn(e);
					pixels[i][y][x] = Float.NaN;
				}
			}
			dp.close();			
		}		
	}
}
