package pointcloud;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Informal.Builder;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.RemoteProxy;
import util.Range2d;

public class Rasterizer extends RemoteProxy {
	private static final Logger log = LogManager.getLogger();
	
	public static final double DEFAULT_POINT_SCALE = 4;

	protected final PointCloud pointcloud;
	protected final RasterDB rasterdb;
	protected final RasterUnitStorage rasterUnit;
	
	private final double point_scale;
	private final double raster_pixel_size;
	
	private Band bandIntensity;
	private Band bandElevation;

	private static final int raster_pixel_per_batch_row = TilePixel.PIXELS_PER_ROW * 32;
	private static final int border_pixels = 3;

	private AttributeSelector selectorIntensity;
	private AttributeSelector selectorElevation;

	public Rasterizer(PointCloud pointcloud, RasterDB rasterdb, double point_scale) {
		this.point_scale = point_scale;
		this.raster_pixel_size = 1d / point_scale;
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
		
		Range2d cellrange = pointcloud.getCellRange();
		DoublePoint celloffset = pointcloud.getCelloffset();
		double cellsize = pointcloud.getCellsize();
		double pointcloud_xmin = (celloffset.x + cellrange.xmin) * cellsize;
		double pointcloud_ymin = (celloffset.y + cellrange.ymin) * cellsize;		
		log.info("set raster to pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin);
		rasterdb.setPixelSize(raster_pixel_size, raster_pixel_size, pointcloud_xmin, pointcloud_ymin);

		selectorElevation = new AttributeSelector();
		selectorElevation.setXY();
		selectorElevation.z = true;

		selectorIntensity = new AttributeSelector();
		selectorIntensity.setXY();
		selectorIntensity.intensity = true;
	}
	
	@Override
	public void process() throws Exception {
		AttributeSelector selector = pointcloud.getSelector();
		if(selector.red) {
			Band bandRed = rasterdb.createBand(TilePixel.TYPE_FLOAT, "red", null);
			setMessage("rasterize red");
			run(bandRed, new AttributeSelector().setXY().setRed(), Rasterizer::processRed);
		}
		if(selector.green) {
			Band bandGreen = rasterdb.createBand(TilePixel.TYPE_FLOAT, "green", null);
			setMessage("rasterize green");
			run(bandGreen, new AttributeSelector().setXY().setGreen(), Rasterizer::processGreen);
		}		
		if(selector.blue) {
			Band bandBlue = rasterdb.createBand(TilePixel.TYPE_FLOAT, "blue", null);
			setMessage("rasterize blue");
			run(bandBlue, new AttributeSelector().setXY().setBlue(), Rasterizer::processBlue);
		}
		if(selector.intensity) {
			this.bandIntensity = rasterdb.createBand(TilePixel.TYPE_FLOAT, "intensity", null);
			setMessage("rasterize intensity");
			run(bandIntensity, selectorIntensity, Rasterizer::processIntensity);
		}
		if(selector.z) {
			this.bandElevation = rasterdb.createBand(TilePixel.TYPE_FLOAT, "elevation", null);
			setMessage("rasterize elevation");
			run(bandElevation, selectorElevation, Rasterizer::processElevation);
		}
	}
	
	@Override
	public void close() {
		bandIntensity = null;
		bandElevation = null;
	}

	private void run(Band selectedBand, AttributeSelector selector, PointProcessing pointProcessing) throws IOException {
		Range2d cellrange = pointcloud.getCellRange();
		if(cellrange == null) {
			return;
		}
		log.info("cellRange: " + cellrange);
		runCellRange(selectedBand, selector, pointProcessing, cellrange);
	}


	private void runCellRange(Band selectedBand, AttributeSelector selector, PointProcessing pointProcessing, Range2d cellrange) throws IOException {
		if(cellrange == null) {
			return;
		}
		log.info("pre  " + cellrange);
		cellrange = pointcloud.getCellRange2dOfSubset(cellrange);
		log.info("post " + cellrange);
		if(cellrange == null) {
			return;
		}
		int cellrangeY = cellrange.getHeight();
		if(cellrangeY > 32) {
			log.info("subdiv Y " + cellrangeY);
			int middleY = (int) ((((long)cellrange.ymin) + ((long)cellrange.ymax)) / 2);
			runCellRange(selectedBand, selector, pointProcessing, new Range2d(cellrange.xmin, cellrange.ymin, cellrange.xmax, middleY));
			runCellRange(selectedBand, selector, pointProcessing, new Range2d(cellrange.xmin, middleY + 1, cellrange.xmax, cellrange.ymax));
			return;
		}
		
		int cellrangeX = cellrange.getWidth();
		if(cellrangeX > 32) {
			log.info("subdiv X " + cellrangeX);
			int middleX = (int) ((((long)cellrange.xmin) + ((long)cellrange.xmax)) / 2);
			runCellRange(selectedBand, selector, pointProcessing, new Range2d(cellrange.xmin, cellrange.ymin, middleX, cellrange.ymax));
			runCellRange(selectedBand, selector, pointProcessing, new Range2d(middleX + 1, cellrange.ymin, cellrange.xmax, cellrange.ymax));
			return;
		}		

		DoublePoint celloffset = pointcloud.getCelloffset();
		double cellsize = pointcloud.getCellsize();
		double cellscale1d = 1 / pointcloud.getCellscale();
		double pointcloud_xmin = (celloffset.x + cellrange.xmin) * cellsize;
		double pointcloud_ymin = (celloffset.y + cellrange.ymin) * cellsize;
		double pointcloud_xmax = (celloffset.x + cellrange.xmax + 1) * cellsize - cellscale1d;
		double pointcloud_ymax = (celloffset.y + cellrange.ymax + 1) * cellsize - cellscale1d;		
		log.info("local pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin+"   pointcloud_xmax " + pointcloud_xmax + " pointcloud_ymax " + pointcloud_ymax);
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
				//log.info("xmin " + xmin + " ymin " + ymin+"   xmax " + xmax + " ymax " + ymax);

				int bxmin = xmin - border_pixels;
				int bymin = ymin - border_pixels;
				int bxmax = xmax + border_pixels;
				int bymax = ymax + border_pixels;

				double qxmin = ref.pixelXToGeo(bxmin);
				double qymin = ref.pixelYToGeo(bymin);
				double qxmax = ref.pixelXToGeo(bxmax + 1) - cellscale1d;
				double qymax = ref.pixelYToGeo(bymax + 1) - cellscale1d;
				//log.info("qxmin " + qxmin + " qymin " + qymin+"   qxmax " + qxmax + " qymax " + qymax);

				int cellCount = pointcloud.countCells(qxmin, qymin, qxmax, qymax);
				log.info("cell count "+cellCount);
				if(cellCount > 0) {
					Stream<PointTable> pointTables = pointcloud.getPointTables(qxmin, qymin, qxmax, qymax, selector);
					float[][] pixels = ProcessingFloat.createEmpty(bxmax - bxmin + 1, bymax - bymin + 1);
					pointProcessing.process(pointTables, qxmin, qymin, pixels, point_scale);
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
		void process(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels, double point_scale);
	}

	private static void processIntensity(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels, double point_scale) {
		pointTables.forEach(p->{
			char[] intensity = p.intensity;
			if(intensity != null) {
				int len = p.rows;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					int x = (int) ((xs[i] - qxmin) * point_scale);
					int y = (int) ((ys[i] - qymin) * point_scale);
					float prev = pixels[y][x];
					float v = intensity[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}

	private static void processElevation(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels, double point_scale) {
		pointTables.forEach(p->{
			double[] z = p.z;
			if(z != null) {
				int len = p.rows;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					//log.info("pos " + xs[i] + "  " + ys[i]);
					int x = (int) ((xs[i] - qxmin) * point_scale);
					int y = (int) ((ys[i] - qymin) * point_scale);
					float prev = pixels[y][x];
					float v = (float) z[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}

	private static void processRed(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels, double point_scale) {
		//log.info("processRed " + qxmin + "  " + qymin);
		pointTables.forEach(p->{
			char[] red = p.red;
			if(red != null) {
				int len = p.rows;
				//log.info("row points " + len);
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					//log.info("processRed point " + xs[i] + "  " + ys[i]);
					int x = (int) ((xs[i] - qxmin) * point_scale);
					int y = (int) ((ys[i] - qymin) * point_scale);
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

	private static void processGreen(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels, double point_scale) {
		pointTables.forEach(p->{
			char[] green = p.green;
			if(green != null) {
				int len = p.rows;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					int x = (int) ((xs[i] - qxmin) * point_scale);
					int y = (int) ((ys[i] - qymin) * point_scale);
					float prev = pixels[y][x];
					float v = green[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
			}
		});
	}

	private static void processBlue(Stream<PointTable> pointTables, double qxmin, double qymin, float[][] pixels, double point_scale) {
		pointTables.forEach(p->{
			char[] blue = p.blue;
			if(blue != null) {
				int len = p.rows;
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					int x = (int) ((xs[i] - qxmin) * point_scale);
					int y = (int) ((ys[i] - qymin) * point_scale);
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
