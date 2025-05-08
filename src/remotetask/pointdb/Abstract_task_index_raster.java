package remotetask.pointdb;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import pointcloud.Rect2d;
import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.process.ProcessingFun;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBandProcessor;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import util.BlokingTaskSubmitter;
import util.Range2d;
import util.Timer;
import util.frame.BooleanFrame;

public abstract class Abstract_task_index_raster extends CancelableRemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final DataProvider2Factory dpFactory;

	public Abstract_task_index_raster(Context ctx, DataProvider2Factory dpFactory) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		this.dpFactory = dpFactory;
	}

	@Override
	public void process() throws IOException {
		String rasterdb_name = task.getString("rasterdb");
		RasterDB rasterdb = broker.getRasterdb(rasterdb_name);
		rasterdb.checkMod(ctx.userIdentity, "task index_raster");

		Band maskBand = null;
		if(task.has("mask_band")) {
			int maskBandIndex = task.getInt("mask_band");
			maskBand = rasterdb.getBandByNumber(maskBandIndex);
		}

		JSONArray indices_text = task.getJSONArray("indices");
		int indices_len = indices_text.length();
		ProcessingFun[] indices = new ProcessingFun[indices_len];
		Band[] bands = new Band[indices_len];
		for (int i = 0; i < indices_len; i++) {
			String index_text = indices_text.getString(i);
			//indices[i] = Functions.getFun(index_text);
			indices[i] = pointdb.indexfuncdsl.Compiler.compile(index_text);
			bands[i] = rasterdb.createBand(TilePixel.TYPE_FLOAT, index_text, null);
		}

		Rect2d pointcloudExtent = dpFactory.getExtent();
		double xmin = pointcloudExtent.xmin;
		double ymin = pointcloudExtent.ymin;
		double xmax = pointcloudExtent.xmax;
		double ymax = pointcloudExtent.ymax;

		JSONArray rect_Text = task.optJSONArray("rect");
		if(rect_Text != null) {
			double rect_xmin = rect_Text.getDouble(0);
			double rect_ymin = rect_Text.getDouble(1);
			double rect_xmax = rect_Text.getDouble(2);
			double rect_ymax = rect_Text.getDouble(3);
			if(xmin < rect_xmin) xmin = rect_xmin;
			if(ymin < rect_ymin) ymin = rect_ymin;
			if(xmax > rect_xmax) xmax = rect_xmax;
			if(ymax > rect_ymax) ymax = rect_ymax;
		}


		log("full geo rect ["+xmin+", "+ymin+", "+xmax+", "+ymax+"]   "+ (xmax-xmin) + " x " +  (ymax-ymin) + " projection units" );

		GeoReference ref = rasterdb.ref();
		int raster_xmin = ref.geoXToPixel(xmin);
		int raster_ymin = ref.geoYToPixel(ymin);
		int raster_xmax = ref.geoXToPixel(xmax);
		int raster_ymax = ref.geoYToPixel(ymax);

		if(maskBand != null) {
			Range2d localRange = rasterdb.getLocalRange(true);
			if(raster_xmin < localRange.xmin) raster_xmin = localRange.xmin;
			if(raster_ymin < localRange.ymin) raster_ymin = localRange.ymin;
			if(raster_xmax > localRange.xmax) raster_xmax = localRange.xmax;
			if(raster_ymax > localRange.ymax) raster_ymax = localRange.ymax;
		}

		log("full raster local rect ["+raster_xmin+", "+raster_ymin+", "+raster_xmax+", "+raster_ymax+"]   "+ (raster_xmax-raster_xmin+1) + " x " +  (raster_ymax-raster_ymin+1) + " = " + (raster_xmax-raster_xmin+1)*(raster_ymax-raster_ymin+1) + " pixel");

		Timer.start("index_raster processing");
		if(maskBand != null) {
			log("use mask band");
		} else {
			log("use NO mask band");	
		}

		if(isCanceled()) {
			throw new RuntimeException("canceled");
		}
		int[] rectCounter = new int[] {0};
		long total_process_pixels = process_rect(dpFactory, rasterdb, ref, raster_xmin, raster_ymin, raster_xmax, raster_ymax, bands, indices, maskBand, true, 0, 0, rectCounter, 0);
		log(total_process_pixels + " total process pixels");
		int total_rects = rectCounter[0];
		rectCounter[0] = 0;
		long processed_pixel = process_rect(dpFactory, rasterdb, ref, raster_xmin, raster_ymin, raster_xmax, raster_ymax, bands, indices, maskBand, false, total_process_pixels, 0, rectCounter, total_rects);		
		Timer timer = Timer.stop("index_raster processing");
		long xsize = raster_xmax - raster_xmin + 1;
		long ysize = raster_ymax - raster_ymin + 1;
		long pixel = xsize * ysize;
		log(timer+"   size " + xsize + " x " + ysize+":  " + processed_pixel + " pixel  of rect pixel " + pixel + ";  " +  (((timer.end - timer.begin))/(processed_pixel)) + " ms/pixel");

		if(isCanceled()) {
			throw new RuntimeException("canceled");
		}
		setMessage("create pyramid");
		rasterdb.rebuildPyramid(true, this);
	}

	private static long MAX_RASTER_BYTES = 268_435_456; // default
	//private static long MAX_RASTER_BYTES = 1_024; // short testing

	public long process_rect(DataProvider2Factory dpFactory, RasterDB rasterdb, GeoReference ref, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, Band[] bands, ProcessingFun[] indices, Band maskBand, boolean dryRun, long total_process_pixels, long processed_pixel, int[] rectCounter, int total_rects) throws IOException {
		long xsize = raster_xmax - raster_xmin + 1;
		long ysize = raster_ymax - raster_ymin + 1;
		long maxPixel = (MAX_RASTER_BYTES / 4) / indices.length;
		long pixel = xsize * ysize;
		if(pixel <= maxPixel || (xsize <= 1 && ysize <= 1)) {
			if(!dryRun) {
				log("part local rect ["+raster_xmin+", "+raster_ymin+", "+raster_xmax+", "+raster_ymax+"]     size " + xsize + " x " + ysize + "  =  " + pixel+" pixel");
			}

			boolean[][] mask = null;
			if(maskBand != null) {
				Range2d range2d = new Range2d(raster_xmin, raster_ymin, raster_xmax, raster_ymax);
				int timestamp = 0;
				TimeBandProcessor processor = new TimeBandProcessor(rasterdb, range2d);
				BooleanFrame maskFrame = processor.getFloatFrame(timestamp, maskBand).toMask();
				mask = maskFrame.data;
			}
			processed_pixel = run_rect(dpFactory, rasterdb.rasterUnit(), ref, raster_xmin, raster_ymin, raster_xmax, raster_ymax, bands, indices, mask, dryRun, total_process_pixels, processed_pixel, rectCounter, total_rects);			
		} else if(xsize >= ysize) {
			int raster_xmid = (int) ((((long)raster_xmin) + ((long)raster_xmax)) >> 1);
			if(!dryRun) {
				log("split rect on x " + raster_xmin + " .. " + raster_xmid + " .. " + raster_xmax);
			}
			processed_pixel = process_rect(dpFactory, rasterdb, ref, raster_xmin, raster_ymin, raster_xmid, raster_ymax, bands, indices, maskBand, dryRun, total_process_pixels, processed_pixel, rectCounter, total_rects);
			processed_pixel = process_rect(dpFactory, rasterdb, ref, raster_xmid + 1, raster_ymin, raster_xmax, raster_ymax, bands, indices, maskBand, dryRun, total_process_pixels, processed_pixel, rectCounter, total_rects);
		} else {
			int raster_ymid = (int) ((((long)raster_ymin) + ((long)raster_ymax)) >> 1);
			if(!dryRun) {
				log("split rect on y " + raster_ymin + " .. " + raster_ymid + " .. " + raster_ymax);
			}
			processed_pixel = process_rect(dpFactory, rasterdb, ref, raster_xmin, raster_ymin, raster_xmax, raster_ymid, bands, indices, maskBand, dryRun, total_process_pixels, processed_pixel, rectCounter, total_rects);
			processed_pixel = process_rect(dpFactory, rasterdb, ref, raster_xmin, raster_ymid + 1, raster_xmax, raster_ymax, bands, indices, maskBand, dryRun, total_process_pixels, processed_pixel, rectCounter, total_rects);
		}
		return processed_pixel;
	}

	public long run_rect(DataProvider2Factory dpFactory, RasterUnitStorage rasterUnitStorage, GeoReference ref, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, Band[] bands, ProcessingFun[] indices, boolean[][] mask, boolean dryRun, long total_process_pixels, long processed_pixel, int[] rectCounter, int total_rects) throws IOException {
		rectCounter[0]++;
		BlokingTaskSubmitter blokingTaskSubmitter = new BlokingTaskSubmitter();

		int indices_len = indices.length;
		int width = raster_xmax - raster_xmin + 1;
		int height = raster_ymax - raster_ymin + 1;
		float[][][] pixels = new float[indices_len][height][width];

		if(!dryRun) {
			for (int i = 0; i < indices_len; i++) {
				float[][] ind = pixels[i];
				for (int y = 0; y < height; y++) {
					float[] row = ind[y];
					for (int x = 0; x < width; x++) {
						row[x] = Float.NaN;
					}
				}
			}		
			Timer.start("part rect");
		}
		
		long rect_processed_pixel = 0;

		for (int y = 0; y < height; y++) {
			if(!dryRun) {
				Timer.start("for line");
			}
			for (int x = 0; x < width; x++) {
				if(mask == null || mask[y][x]) {
					if(!dryRun) {
						double xgeo = ref.pixelXToGeo(raster_xmin + x);
						double ygeo = ref.pixelYToGeo(raster_ymin + y);
						double xgeo1 = ref.pixelXToGeo(raster_xmin + x + 1);
						double ygeo1 = ref.pixelYToGeo(raster_ymin +y + 1);
						long pxmin = PdbConst.to_utmm(xgeo);
						long pymin = PdbConst.to_utmm(ygeo);
						long pxmax = PdbConst.to_utmm(xgeo1) - 1;
						long pymax = PdbConst.to_utmm(ygeo1) - 1;
						Rect pRect = Rect.of_UTMM(pxmin, pymin, pxmax, pymax);
						IndexRasterizerPixelTask pixeltask = new IndexRasterizerPixelTask(blokingTaskSubmitter, dpFactory, pRect, indices, pixels, x, y);
						if(isCanceled()) {
							throw new RuntimeException("canceled");
						}
						blokingTaskSubmitter.submit(pixeltask);
					}
					rect_processed_pixel++;
				}
			}
			if(!dryRun) {
				//Logger.info(Timer.stop("current line") + "     " + (y+1) +" of "+height);
				setMessage("approx. " + Timer.stop("for line") + "     " + (y+1) +" of "+height + " lines in part rect " + rectCounter[0] + " of total " + total_rects + "; " + (processed_pixel + rect_processed_pixel) + " pixel of total " + total_process_pixels + "; " +  ((processed_pixel + rect_processed_pixel)*100)/total_process_pixels + " %");
			}
		}
		processed_pixel += rect_processed_pixel;


		if(!dryRun) {
			blokingTaskSubmitter.finish();
			Timer timer = Timer.stop("part rect");
			log(timer+"   size " + width + " x " + height+":  " + rect_processed_pixel + " pixel  of " + (width*height) + "; " +  (((timer.end - timer.begin))/(rect_processed_pixel)) + " ms/pixel in rect " + rectCounter[0] + " of total " + total_rects);

			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
			log("write part rect local min ["+raster_xmin + ", " + raster_ymin + "]     geo min [" + ref.pixelXToGeo(raster_xmin) + ", " + ref.pixelYToGeo(raster_ymin) + "] in rect " + rectCounter[0] + " of total " + total_rects);
			for (int i = 0; i < indices_len; i++) {
				ProcessingFloat.writeMerge(rasterUnitStorage, 0, bands[i], pixels[i], raster_ymin, raster_xmin);
			}
			rasterUnitStorage.commit();
			log(processed_pixel + " pixel of total pixel " + total_process_pixels + "; " +  (processed_pixel*100)/total_process_pixels + " % done");
		}
		return processed_pixel;
	}
}
