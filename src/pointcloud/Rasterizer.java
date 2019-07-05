package pointcloud;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Informal.Builder;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnit;
import util.Range2d;

public class Rasterizer {
	private static final Logger log = LogManager.getLogger();

	protected final PointCloud pointcloud;
	protected final RasterDB rasterdb;
	protected final RasterUnit rasterUnit;
	private Band bandIntensity;
	private  Band bandElevation;

	private static final double tile_scale = 4;
	private static final double raster_pixel_size = 1d / tile_scale;
	private static final int raster_pixel_per_batch_row = TilePixel.PIXELS_PER_ROW * 32;
	private static final int border_pixels = 3;

	private AttributeSelector selectorIntensity;
	private AttributeSelector selectorElevation;

	public Rasterizer(PointCloud pointcloud, RasterDB rasterdb) {
		this.pointcloud = pointcloud;
		this.rasterdb = rasterdb;
		this.rasterUnit = rasterdb.rasterUnit();
		if(!rasterdb.rasterUnit().isEmpty()) {
			throw new RuntimeException("target RasterDB not empty: " + rasterdb.config.getName());
		}
		rasterdb.setCode(pointcloud.getCode());
		rasterdb.setProj4(pointcloud.getProj4());
		rasterdb.associated.setPointCloud(pointcloud.getName());
		Builder informal = rasterdb.informal().toBuilder();
		informal.description = "ratserized poindcloud " + pointcloud.getName();
		rasterdb.setInformal(informal.build());
		rasterdb.writeMeta();
		
		pointcloud.setAssociatedRasterDB(rasterdb.config.getName());

		selectorElevation = new AttributeSelector();
		selectorElevation.setXY();
		selectorElevation.z = true;

		selectorIntensity = new AttributeSelector();
		selectorIntensity.setXY();
		selectorIntensity.intensity = true;
	}

	public void run() {
		AttributeSelector selector = pointcloud.getSelector();
		if(selector.red) {
			Band bandRed = rasterdb.createBand(TilePixel.TYPE_FLOAT, "red", null);
			log.info("rasterize red");
			run(bandRed, new AttributeSelector().setXY().setRed(), Rasterizer::processRed);
		}
		if(selector.green) {
			Band bandGreen = rasterdb.createBand(TilePixel.TYPE_FLOAT, "green", null);
			log.info("rasterize green");
			run(bandGreen, new AttributeSelector().setXY().setGreen(), Rasterizer::processGreen);
		}		
		if(selector.blue) {
			Band bandBlue = rasterdb.createBand(TilePixel.TYPE_FLOAT, "blue", null);
			log.info("rasterize blue");
			run(bandBlue, new AttributeSelector().setXY().setBlue(), Rasterizer::processBlue);
		}
		if(selector.intensity) {
			this.bandIntensity = rasterdb.createBand(TilePixel.TYPE_FLOAT, "intensity", null);
			log.info("rasterize intensity");
			run(bandIntensity, selectorIntensity, Rasterizer::processIntensity);
		}
		if(selector.z) {
			this.bandElevation = rasterdb.createBand(TilePixel.TYPE_FLOAT, "elevation", null);
			log.info("rasterize elevation");
			run(bandElevation, selectorElevation, Rasterizer::processElevation);
		}
	}


	private void run(Band selectedBand, AttributeSelector selector, PointProcessing pointProcessing) {
		Range2d cellrange = pointcloud.getCellRange();
		DoublePoint celloffset = pointcloud.getCelloffset();
		double cellsize = pointcloud.getCellsize();
		double cellscale1d = 1 / pointcloud.getCellscale();
		double pointcloud_xmin = (celloffset.x + cellrange.xmin) * cellsize;
		double pointcloud_ymin = (celloffset.y + cellrange.ymin) * cellsize;
		double pointcloud_xmax = (celloffset.x + cellrange.xmax + 1) * cellsize - cellscale1d;
		double pointcloud_ymax = (celloffset.y + cellrange.ymax + 1) * cellsize - cellscale1d;		
		log.info("pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin+"   pointcloud_xmax " + pointcloud_xmax + " pointcloud_ymax " + pointcloud_ymax);
		rasterdb.setPixelSize(raster_pixel_size, raster_pixel_size, pointcloud_xmin, pointcloud_ymin);
		GeoReference ref = rasterdb.ref();
		int raster_xmin = ref.geoXToPixel(pointcloud_xmin);
		int raster_ymin = ref.geoYToPixel(pointcloud_ymin);
		int raster_xmax = ref.geoXToPixel(pointcloud_xmax);
		int raster_ymax = ref.geoYToPixel(pointcloud_ymax);
		log.info("raster_xmin " + raster_xmin + " raster_ymin " + raster_ymin+"   raster_xmax " + raster_xmax + " raster_ymax " + raster_ymax);

		int ymin = raster_ymin;
		while(ymin <= raster_ymax) {
			int ymax = ymin + raster_pixel_per_batch_row - 1;
			int xmin = raster_xmin;
			while(xmin <= raster_xmax) {
				int xmax = xmin + raster_pixel_per_batch_row - 1;
				log.info("xmin " + xmin + " ymin " + ymin+"   xmax " + xmax + " ymax " + ymax);

				int bxmin = xmin - border_pixels;
				int bymin = ymin - border_pixels;
				int bxmax = xmax + border_pixels;
				int bymax = ymax + border_pixels;

				double qxmin = ref.pixelXToGeo(bxmin);
				double qymin = ref.pixelYToGeo(bymin);
				double qxmax = ref.pixelXToGeo(bxmax + 1) - cellscale1d;
				double qymax = ref.pixelYToGeo(bymax + 1) - cellscale1d;
				log.info("qxmin " + qxmin + " qymin " + qymin+"   qxmax " + qxmax + " qymax " + qymax);

				int cellCount = pointcloud.countCells(qxmin, qymin, qxmax, qymax);
				log.info("cell count "+cellCount);
				if(cellCount > 0) {
					Stream<PointTable> pointTables = pointcloud.getPointTables(qxmin, qymin, qxmax, qymax, selector);
					float[][] pixels = ProcessingFloat.createEmpty(bxmax - bxmin + 1, bymax - bymin + 1);
					pointProcessing.process(pointTables, qxmin, qymin, pixels);
					pointdb.Rasterizer.fill(pixels);
					pixels = removeBorder(pixels, border_pixels);
					ProcessingFloat.writeMerge(rasterUnit, 0, selectedBand, pixels, ymin, xmin);
					rasterUnit.commit();
					log.info("committed");
				}
				xmin += raster_pixel_per_batch_row;
			}

			ymin += raster_pixel_per_batch_row;
		}
	}

	private interface PointProcessing {
		void process(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels);
	}

	private static void processIntensity(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels) {
		pointTables.forEach(p->{
			char[] intensity = p.intensity;
			if(intensity != null) {
				int len = p.rows;
				double scale = Rasterizer.tile_scale;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					int x = (int) ((xs[i] - qxmin) * scale);
					int y = (int) ((ys[i] - qymin) * scale);
					float prev = pixels[y][x];
					float v = intensity[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}

	private static void processElevation(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels) {
		pointTables.forEach(p->{
			double[] z = p.z;
			if(z != null) {
				int len = p.rows;
				double scale = Rasterizer.tile_scale;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					//log.info("pos " + xs[i] + "  " + ys[i]);
					int x = (int) ((xs[i] - qxmin) * scale);
					int y = (int) ((ys[i] - qymin) * scale);
					float prev = pixels[y][x];
					float v = (float) z[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}
	
	private static void processRed(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels) {
		//log.info("processRed " + qxmin + "  " + qymin);
		pointTables.forEach(p->{
			char[] red = p.red;
			if(red != null) {
				int len = p.rows;
				//log.info("row points " + len);
				double scale = Rasterizer.tile_scale;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					//log.info("processRed point " + xs[i] + "  " + ys[i]);
					int x = (int) ((xs[i] - qxmin) * scale);
					int y = (int) ((ys[i] - qymin) * scale);
					float prev = pixels[y][x];
					float v = red[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
				//log.info("row points FINISH");
			}
		});
	}
	
	private static void processGreen(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels) {
		pointTables.forEach(p->{
			char[] green = p.green;
			if(green != null) {
				int len = p.rows;
				double scale = Rasterizer.tile_scale;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					int x = (int) ((xs[i] - qxmin) * scale);
					int y = (int) ((ys[i] - qymin) * scale);
					float prev = pixels[y][x];
					float v = green[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}
	
	private static void processBlue(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels) {
		pointTables.forEach(p->{
			char[] blue = p.blue;
			if(blue != null) {
				int len = p.rows;
				double scale = Rasterizer.tile_scale;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					int x = (int) ((xs[i] - qxmin) * scale);
					int y = (int) ((ys[i] - qymin) * scale);
					float prev = pixels[y][x];
					float v = blue[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}

	private static float[][] removeBorder(float[][] pixels, int border) {
		int xlen = pixels[0].length - (border << 1);
		int ylen = pixels.length - (border << 1);
		float[][] result = new float[ylen][xlen];
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(pixels[y + border], border, result[y], 0, xlen);
		}
		return result;
	}
}
