package pointdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Informal.Builder;
import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.process.DataProvider2;
import pointdb.process.ProcessingFun;
import pointdb.subsetdsl.Region;
import rasterdb.Band;
import rasterdb.ProcessingFloat;
import rasterdb.RasterDB;
import rasterdb.TilePixel;
import rasterunit.RasterUnit;

public class IndexRasterizer {
	private static final Logger log = LogManager.getLogger();

	private final PointDB pointdb;
	private final RasterDB rasterdb;
	private final RasterUnit rasterUnit;

	private final Rect processingRect;
	private long processing_pixel_size;

	public IndexRasterizer(PointDB pointdb, RasterDB rasterdb, double pixel_size, Rect processingRect) {
		this.pointdb = pointdb;
		this.rasterdb = rasterdb;
		this.processing_pixel_size = PdbConst.to_utmm(pixel_size);
		double raster_pixel_size = PdbConst.utmm_to_double(processing_pixel_size);
		rasterdb.setPixelSize(raster_pixel_size, raster_pixel_size, processingRect.getUTMd_min_x(), processingRect.getUTMd_min_y());
		rasterdb.setProj4(pointdb.config.getProj4());
		rasterdb.setCode("EPSG:" + pointdb.config.getEPSG());
		rasterdb.associated.setPointDB(pointdb.config.name);
		Builder informal = rasterdb.informal().toBuilder();
		informal.description = "ratserized poind cloud";
		rasterdb.setInformal(informal.build());
		rasterdb.writeMeta();
		this.rasterUnit = rasterdb.rasterUnit();
		this.processingRect = processingRect;
	}

	public void process(ProcessingFun processingFun) {		

		long xmin = processingRect.utmm_min_x;
		long ymin = processingRect.utmm_min_y;
		long xmax = processingRect.utmm_max_x;
		long ymax = processingRect.utmm_max_y;

		int width = 0;
		long wx = xmin;
		while(wx <= xmax) {
			width++;
			wx += processing_pixel_size;
		}

		int height = 0;
		long hy = ymin;
		while(hy <= ymax) {
			height++;
			hy += processing_pixel_size;
		}

		log.info("width "+width+"  height "+height);
		float[][] pixels = new float[height][width];

		long y = ymin;
		long cnt = 0;

		long processing_pixel_add = processing_pixel_size - 1;

				int py = 0;
		while(y <= ymax) {
			log.info("row " + py + " of " + height);
			long x = xmin;
			int px = 0;			
			while(x <= xmax) {
				Rect rect = Rect.of_UTMM(x, y, x + processing_pixel_add, y + processing_pixel_add);
				DataProvider2 dp = new DataProvider2(pointdb, Region.ofRect(rect));
				pixels[py][px] = (float) processingFun.process(dp);
				cnt++;
				x += processing_pixel_size;
				px++;
			}
			y += processing_pixel_size;
			py++;
		}

		log.info("cnt " + cnt);
		Band band = rasterdb.createBand(TilePixel.TYPE_FLOAT, "index_rasterized", null);
		ProcessingFloat.writeMerge(rasterUnit, 0, band, pixels, 0, 0);
		rasterUnit.commit();

	}

}
