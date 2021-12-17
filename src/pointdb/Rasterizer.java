package pointdb;

import java.io.IOException;


import org.tinylog.Logger;

import broker.Informal;
import broker.Informal.Builder;
import pointdb.base.PdbConst;
import pointdb.base.Point;
import pointdb.base.Rect;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;
import pointdb.processing.tilekey.StatisticsCollector;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;

public class Rasterizer {
	

	protected final PointDB pointdb;
	protected final RasterDB rasterdb;
	protected final RasterUnitStorage rasterUnit;
	public final Band bandIntensity;
	public final Band bandElevation;

	private static final int tile_scale = 2;
	private static final int raster_pixel_size_mm = PdbConst.LOCAL_SCALE_FACTOR / tile_scale;
	private static final double raster_pixel_size = raster_pixel_size_mm / PdbConst.LOCAL_SCALE_FACTORd;

	public Rasterizer(PointDB pointdb, RasterDB rasterdb) {
		this.pointdb = pointdb;
		this.rasterdb = rasterdb;
		rasterdb.setPixelSize(raster_pixel_size, raster_pixel_size, 0, 0);
		rasterdb.setProj4(pointdb.config.getProj4());
		rasterdb.setCode("EPSG:" + pointdb.config.getEPSG());
		rasterdb.associated.setPointDB(pointdb.config.name);
		rasterdb.associated.setPoi_groups(pointdb.config.getPoiGroupNames());
		rasterdb.associated.setRoi_groups(pointdb.config.getRoiGroupNames());
		Informal pinfo = pointdb.config.informal();
		Builder informal = rasterdb.informal().toBuilder();
		if(pinfo.hasTitle()) {
			informal.title = pinfo.title + " (rasterized)";
		}
		informal.description = pinfo.description.isEmpty() ? "ratserized poind cloud" : pinfo.description + " (rasterized)";
		informal.tags = pinfo.tags;
		rasterdb.setInformal(informal.build());
		rasterdb.setACL(pointdb.config.getAcl());
		rasterdb.writeMeta();
		this.rasterUnit = rasterdb.rasterUnit();
		this.bandIntensity = rasterdb.createBand(TilePixel.TYPE_FLOAT, "rasterized intensity", null);
		this.bandElevation = rasterdb.createBand(TilePixel.TYPE_FLOAT, "rasterized elevation", null);
	}

	private class Processor implements TileConsumer {

		private int pointdbTileReadCount = 0;

		private final int xmin;
		private final int ymin;
		private final int xmax;
		private final int ymax;

		private final Band band;

		private float[][] pixels;

		public Processor(Rect rect, Band band) {
			this(rect.getInteger_UTM_min_x(), rect.getInteger_UTM_min_y(), rect.getInteger_UTM_max_x(), rect.getInteger_UTM_max_y(), band);
		}

		public Processor(int xmin, int ymin, int xmax, int ymax, Band band) {
			this.xmin = xmin;
			this.ymin = ymin;
			this.xmax = xmax;
			this.ymax = ymax;
			this.band = band;
			int xlen = (xmax - xmin + PdbConst.UTM_TILE_SIZE) * tile_scale + 1;
			int ylen = (ymax - ymin + PdbConst.UTM_TILE_SIZE) * tile_scale + 1;
			this.pixels = new float[ylen][xlen];
			for (int y = 0; y < ylen; y++) {
				for (int x = 0; x < xlen; x++) {
					pixels[y][x] = Float.NaN;
				}
			}
		}

		@Override
		public void nextTile(pointdb.base.Tile tile) {
			pointdbTileReadCount++;
			int pos_x = (tile.meta.x - xmin) * tile_scale;
			int pos_y = (tile.meta.y - ymin) * tile_scale;

			Point[] points = tile.points;
			int len = points.length;
			if(band.index == bandIntensity.index) {
				for (int i = 0; i < len; i++) {
					Point p = points[i];
					int x = pos_x + p.x / raster_pixel_size_mm;
					int y = pos_y + p.y / raster_pixel_size_mm;
					float v = p.intensity;
					float prev = pixels[y][x];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			} else {
				for (int i = 0; i < len; i++) {
					Point p = points[i];
					int x = pos_x + p.x / raster_pixel_size_mm;
					int y = pos_y + p.y / raster_pixel_size_mm;
					float v = p.z / 1000f;
					float prev = pixels[y][x];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		}

		public void write() throws IOException {
			if(pointdbTileReadCount > 0) {
				Logger.info("read");
				fill(pixels);
				int rasterdbTileWriteCount = ProcessingFloat.writeMerge(rasterUnit, 0, band, pixels, ymin * tile_scale, xmin * tile_scale);
				Logger.info("written "+"pointdbTileReadCount "+pointdbTileReadCount+"    "+"rasterdbTileWriteCount "+rasterdbTileWriteCount+"    "+xmin+"  "+xmax+"          "+((xmax - xmin) / PdbConst.UTM_TILE_SIZE + 1) + "        " + ymin+"  "+ymax+"          "+((ymax - ymin) / PdbConst.UTM_TILE_SIZE + 1));
				rasterUnit.commit();
				Logger.info("committed");
			}
		}
	}

	public void run(Band band) throws IOException {
		Statistics stat = pointdb.tileMetaProducer(null).toStatistics();
		Logger.info(stat.tile_x_min+"  "+stat.tile_x_max+"          "+(stat.tile_x_max - stat.tile_x_min + PdbConst.UTM_TILE_SIZE));
		Logger.info(stat.tile_y_min+"  "+stat.tile_y_max+"          "+(stat.tile_y_max - stat.tile_y_min + PdbConst.UTM_TILE_SIZE));


		int xmin = stat.tile_x_min;
		int ymin = stat.tile_y_min;
		int xmax = stat.tile_x_max;
		int ymax = stat.tile_y_max;
		int x_tile_range = (xmax - xmin) / PdbConst.UTM_TILE_SIZE + 1;

		int cymin = ymin;

		int min_edge_tiles = 16;
		int max_batch_tile_count = 4096;
		while(cymin <= ymax) {
			int y_batch_tile_count = max_batch_tile_count / x_tile_range;
			if(y_batch_tile_count < min_edge_tiles) {
				y_batch_tile_count = min_edge_tiles;
			}			
			int cymax = cymin + (y_batch_tile_count - 1) * PdbConst.UTM_TILE_SIZE;
			if(cymax > ymax) {
				cymax = ymax;
			}
			y_batch_tile_count = (cymax - cymin) / PdbConst.UTM_TILE_SIZE + 1;
			int cxmin = xmin;			
			while(cxmin <= xmax) {
				int x_batch_tile_size = max_batch_tile_count / y_batch_tile_count;
				if(x_batch_tile_size == 0) {
					x_batch_tile_size = 1;
				}
				int cxmax = cxmin + (x_batch_tile_size - 1) * PdbConst.UTM_TILE_SIZE;
				if(cxmax > xmax) {
					cxmax = xmax;
				}				
				//Logger.info(cxmin+"  "+cxmax+"          "+((cxmax - cxmin) / PdbConst.UTM_TILE_SIZE + 1) + "        " + cymin+"  "+cymax+"          "+((cymax - cymin) / PdbConst.UTM_TILE_SIZE + 1));
				Rect rect = Rect.of_UTM(cxmin, cymin, cxmax, cymax);
				StatisticsCollector keyStat = StatisticsCollector.collect(pointdb.tileKeyProducer(rect));
				if(keyStat.tile_count > 0) {
					Rect qRect = keyStat.toRect();
					TileProducer tileProducer = pointdb.tileProducer(qRect);		
					Processor processor = new Processor(qRect, band);
					tileProducer.produce(processor);
					processor.write();
				}
				cxmin = cxmax + PdbConst.UTM_TILE_SIZE;
			}			
			cymin = cymax + PdbConst.UTM_TILE_SIZE;
		}
	}

	public static void fill(float[][] pixels) {
		float[][] src = copy(pixels);
		int w = pixels[0].length - 3;
		int h = pixels.length - 3;
		for (int y = 3; y < h; y++) {
			float[] src_row = src[y];
			for (int x = 3; x < w; x++) {
				if(!Float.isFinite(src_row[x])) {
					float v = fill1(src, x, y);
					if(Float.isFinite(v)) {
						pixels[y][x] = v;
					} else {
						v = fill2(src, x, y);
						if(Float.isFinite(v)) {
							pixels[y][x] = v;
						} else {
							pixels[y][x] = fill3(src, x, y);
						}
					}
				}
			}
		}
	}

	private static float fill1(float[][] pixels, int x, int y) {
		float sum = 0;
		int cnt = 0;
		{
			float v = pixels[y - 1][x - 1];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y - 1][x];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y - 1][x + 1];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y][x - 1];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y][x + 1];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y + 1][x - 1];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y + 1][x];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		{
			float v = pixels[y + 1][x + 1];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}
		return sum / cnt;
	}

	private static float fill2(float[][] pixels, int x, int y) {
		float sum = 0;
		int cnt = 0;
		float[] top_row = pixels[y - 2];
		float[] bottom_row = pixels[y + 2];
		for(int i = x - 2; i <= x + 2; i++) {
			{
				float v = top_row[i];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
			{
				float v = bottom_row[i];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}
		int left_x = x - 2;
		int right_x = x + 2;
		for(int i = y - 1; i <= y + 1; i++) {
			{
				float v = pixels[i][left_x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
			{
				float v = pixels[i][right_x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}
		return sum / cnt;
	}

	private static float fill3(float[][] pixels, int x, int y) {
		float sum = 0;
		int cnt = 0;
		float[] top_row = pixels[y - 3];
		float[] bottom_row = pixels[y + 3];
		for(int i = x - 3; i <= x + 3; i++) {
			{
				float v = top_row[i];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
			{
				float v = bottom_row[i];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}
		int left_x = x - 3;
		int right_x = x + 3;
		for(int i = y - 2; i <= y + 2; i++) {
			{
				float v = pixels[i][left_x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
			{
				float v = pixels[i][right_x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}
		return sum / cnt;
	}

	public static float[][] copy(float[][] src) {
		int len0 = src[0].length;
		int len = src.length;
		float[][] dst = new float[len][len0];
		for (int i = 0; i < len; i++) {
			System.arraycopy(src[i], 0, dst[i], 0, len0);
		}
		return dst;
	}

}
