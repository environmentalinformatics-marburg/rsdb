package pointcloud;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import org.tinylog.Logger;

import broker.Informal.Builder;
import broker.TimeSlice;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.CancelableRemoteProxy;
import util.Range2d;

public class Rasterizer extends CancelableRemoteProxy {	

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

	private final String[] processing_bands;

	public Rasterizer(PointCloud pointcloud, RasterDB rasterdb, double point_scale, String[] processing_bands) {
		this.point_scale = point_scale;
		this.processing_bands = processing_bands;
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
		Logger.info("set raster to pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin);
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

		LinkedHashSet<String> processing_bandSet = new LinkedHashSet<String>();
		String[] bands = processing_bands != null ? processing_bands : new String[] {"red", "green", "blue", "intensity", "elevation"}; 
		for(String processing_band : bands) {
			switch(processing_band) {
			case "red":
			case "green":
			case "blue":
			case "intensity":
			case "elevation":
				processing_bandSet.add(processing_band);
				break;
			default:
				throw new RuntimeException("unknown processing_band: " + processing_band);
			}
		}		

		if(pointcloud.timeMapReadonly.isEmpty()) {
			int t = 0;
			process(t, processing_bandSet, selector);
		} else {
			for(TimeSlice timeSlice : pointcloud.timeMapReadonly.values()) {
				rasterdb.setTimeSlice(timeSlice);
				int t = timeSlice.id;
				process(t, processing_bandSet, selector);			
			}
		}		
	}

	public Band getOrCreateBand(String title) {
		Band band = rasterdb.getBandByTitle(title);
		if(band == null) {
			band = rasterdb.createBand(TilePixel.TYPE_FLOAT, title, null);
		}
		return band;
	}

	public void process(int t, LinkedHashSet<String> processing_bandSet, AttributeSelector selector) throws Exception {
		for(String processing_band : processing_bandSet) {
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
			switch(processing_band) {
			case "red":
				if(selector.red) {
					Band bandRed = getOrCreateBand("red");										
					setMessage("rasterize red   t=" + t);
					run(t, bandRed, new AttributeSelector().setXY().setRed(), Rasterizer::processRed);
				}
				break;
			case "green":
				if(selector.green) {
					Band bandGreen = getOrCreateBand("green");
					setMessage("rasterize green   t=" + t);
					run(t, bandGreen, new AttributeSelector().setXY().setGreen(), Rasterizer::processGreen);
				}
				break;
			case "blue":
				if(selector.blue) {
					Band bandBlue = getOrCreateBand("blue");
					setMessage("rasterize blue   t=" + t);
					run(t, bandBlue, new AttributeSelector().setXY().setBlue(), Rasterizer::processBlue);
				}
				break;
			case "intensity":
				if(selector.intensity) {
					this.bandIntensity = getOrCreateBand("intensity");
					setMessage("rasterize intensity   t=" + t);
					run(t, bandIntensity, selectorIntensity, Rasterizer::processIntensity);
				}
				break;
			case "elevation":
				if(selector.z) {
					this.bandElevation = getOrCreateBand("elevation");
					setMessage("rasterize elevation   t=" + t);
					run(t, bandElevation, selectorElevation, Rasterizer::processElevation);
				}
				break;
			default:
				throw new RuntimeException("unknown processing_band: " + processing_band);
			}
		}

	}

	@Override
	public void close() {
		bandIntensity = null;
		bandElevation = null;
	}

	private void run(int t, Band selectedBand, AttributeSelector selector, PointProcessing pointProcessing) throws IOException {
		Range2d cellrange = pointcloud.getCellRange();
		if(cellrange == null) {
			return;
		}
		Logger.info("cellRange: " + cellrange);
		runCellRange(selectedBand, selector, pointProcessing, t, cellrange);
	}


	private void runCellRange(Band selectedBand, AttributeSelector selector, PointProcessing pointProcessing, int t, Range2d cellrange) throws IOException {
		if(cellrange == null) {
			return;
		}
		if(isCanceled()) {
			throw new RuntimeException("canceled");
		}
		Logger.info("pre  " + cellrange);
		cellrange = pointcloud.getCellRange2dOfSubset(cellrange);
		Logger.info("post " + cellrange);
		if(cellrange == null) {
			return;
		}
		int cellrangeY = cellrange.getHeight();
		if(cellrangeY > 32) {
			Logger.info("subdiv Y " + cellrangeY);
			int middleY = (int) ((((long)cellrange.ymin) + ((long)cellrange.ymax)) / 2);
			runCellRange(selectedBand, selector, pointProcessing, t, new Range2d(cellrange.xmin, cellrange.ymin, cellrange.xmax, middleY));
			runCellRange(selectedBand, selector, pointProcessing, t, new Range2d(cellrange.xmin, middleY + 1, cellrange.xmax, cellrange.ymax));
			return;
		}

		int cellrangeX = cellrange.getWidth();
		if(cellrangeX > 32) {
			Logger.info("subdiv X " + cellrangeX);
			int middleX = (int) ((((long)cellrange.xmin) + ((long)cellrange.xmax)) / 2);
			runCellRange(selectedBand, selector, pointProcessing, t, new Range2d(cellrange.xmin, cellrange.ymin, middleX, cellrange.ymax));
			runCellRange(selectedBand, selector, pointProcessing, t, new Range2d(middleX + 1, cellrange.ymin, cellrange.xmax, cellrange.ymax));
			return;
		}		

		DoublePoint celloffset = pointcloud.getCelloffset();
		double cellsize = pointcloud.getCellsize();
		double cellscale1d = 1 / pointcloud.getCellscale();
		double pointcloud_xmin = (celloffset.x + cellrange.xmin) * cellsize;
		double pointcloud_ymin = (celloffset.y + cellrange.ymin) * cellsize;
		double pointcloud_xmax = (celloffset.x + cellrange.xmax + 1) * cellsize - cellscale1d;
		double pointcloud_ymax = (celloffset.y + cellrange.ymax + 1) * cellsize - cellscale1d;		
		Logger.info("local pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin+"   pointcloud_xmax " + pointcloud_xmax + " pointcloud_ymax " + pointcloud_ymax);
		GeoReference ref = rasterdb.ref();
		int raster_xmin = ref.geoXToPixel(pointcloud_xmin);
		int raster_ymin = ref.geoYToPixel(pointcloud_ymin);
		int raster_xmax = ref.geoXToPixel(pointcloud_xmax);
		int raster_ymax = ref.geoYToPixel(pointcloud_ymax);
		Logger.info("raster_xmin " + raster_xmin + " raster_ymin " + raster_ymin+"   raster_xmax " + raster_xmax + " raster_ymax " + raster_ymax);

		int ymin = raster_ymin;
		while(ymin <= raster_ymax) {
			int ymax = ymin + raster_pixel_per_batch_row - 1;
			int xmin = raster_xmin;
			while(xmin <= raster_xmax) {
				int xmax = xmin + raster_pixel_per_batch_row - 1;
				//Logger.info("xmin " + xmin + " ymin " + ymin+"   xmax " + xmax + " ymax " + ymax);

				int bxmin = xmin - border_pixels;
				int bymin = ymin - border_pixels;
				int bxmax = xmax + border_pixels;
				int bymax = ymax + border_pixels;

				double qxmin = ref.pixelXToGeo(bxmin);
				double qymin = ref.pixelYToGeo(bymin);
				double qxmax = ref.pixelXToGeo(bxmax + 1) - cellscale1d;
				double qymax = ref.pixelYToGeo(bymax + 1) - cellscale1d;
				//Logger.info("qxmin " + qxmin + " qymin " + qymin+"   qxmax " + qxmax + " qymax " + qymax);

				int cellCount = pointcloud.countCells(t, qxmin, qymin, qxmax, qymax);
				Logger.info("cell count "+cellCount);
				if(cellCount > 0) {
					Stream<PointTable> pointTables = pointcloud.getPointTables(t, qxmin, qymin, qxmax, qymax, selector);
					float[][] pixels = ProcessingFloat.createEmpty(bxmax - bxmin + 1, bymax - bymin + 1);
					pointProcessing.process(pointTables, qxmin, qymin, pixels, point_scale);
					pointdb.Rasterizer.fill(pixels);
					pixels = removeBorder(pixels, border_pixels);
					ProcessingFloat.writeMerge(rasterUnit, t, selectedBand, pixels, ymin, xmin);
					rasterUnit.commit();
					Logger.info("committed");
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
					//Logger.info("pos " + xs[i] + "  " + ys[i]);
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
		//Logger.info("processRed " + qxmin + "  " + qymin);
		pointTables.forEach(p->{
			char[] red = p.red;
			if(red != null) {
				int len = p.rows;
				//Logger.info("row points " + len);
				double[] xs = p.x;
				double[] ys = p.y;
				for (int i = 0; i < len; i++) {
					//Logger.info("processRed point " + xs[i] + "  " + ys[i]);
					int x = (int) ((xs[i] - qxmin) * point_scale);
					int y = (int) ((ys[i] - qymin) * point_scale);
					float prev = pixels[y][x];
					float v = red[i];
					if(!Float.isFinite(prev) || prev < v) {
						pixels[y][x] = v;
					}
				}
				//Logger.info("row points FINISH");
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
