package server.api.pointclouds;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import broker.Broker;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.PointCloud;
import pointcloud.PointCountRaster;
import pointcloud.PointRaster;
import pointcloud.PointTable;
import pointcloud.Rect2d;
import rasterdb.node.ProcessorNode_gap_filling;
import rasterdb.tile.ProcessingDouble;
import server.api.pointdb.JsWriter;
import util.Receiver;
import util.ResponseReceiver;
import util.StreamReceiver;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;
import util.tiff.TiffBand;
import util.tiff.TiffWriter;

public class APIHandler_raster {
	private static final Logger log = LogManager.getLogger();

	//private final Broker broker;

	private final static double SMALL_VALUE = 0.000001d;

	public static final String[][] RASTER_TYPES = new String[][] {
		{"dtm", "Digital Terrain Model"},
		{"dsm", "Digital Surface Model"},
		{"chm", "Canopy Height Model"},
		{"point_count", "count of points per pixel"},
		{"pulse_count", "count of laser puleses per pixel"},
	};

	public APIHandler_raster(Broker broker) {
		//this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, HttpServletResponse response) throws IOException {
		String extText = request.getParameter("ext");
		if(extText == null) {
			throw new RuntimeException("missing parameter 'ext'");
		}
		String[] ext = extText.split(" ");
		if(ext.length != 4) {
			throw new RuntimeException("parameter error in 'ext': "+extText);
		}
		double req_xmin = Double.parseDouble(ext[0]);
		double req_ymin = Double.parseDouble(ext[1]);
		double req_xmax = Double.parseDouble(ext[2]);
		double req_ymax = Double.parseDouble(ext[3]);
		log.info("req "+req_xmin+" "+req_ymin+" "+req_xmax+" "+req_ymax);

		String resText = request.getParameter("res");
		double res0 = 1;
		if(resText != null) {
			res0 = Double.parseDouble(resText);
		}
		final double res = res0;

		String fillText = request.getParameter("fill");
		int fill0 = 0;
		if(fillText != null) {
			fill0 = Integer.parseInt(fillText);
			if(fill0 < 0 || fill0 > 100) {
				throw new RuntimeException("fill value out of range (0 to 100): " + fill0);
			}
		}
		final int fill = fill0;

		String typeText = request.getParameter("type");
		String type0 = "count";
		if(typeText != null) {
			type0 = typeText.toLowerCase();
		}
		final String type = type0;

		if(format.equals("zip")) {			
			String tileFormat = "tiff";
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			Receiver receiver = new StreamReceiver(zipOutputStream);			
			double tileSize = 1000d;
			Rect2d req_rect = new Rect2d(req_xmin, req_ymin, req_xmax, req_ymax);
			req_rect.tiled(tileSize, tileSize, (long xtile, long ytile, Rect2d tile_rect) -> {
				String tileFilename = "tile_" + xtile + "_" + ytile + ".tiff";
				try {
					zipOutputStream.putNextEntry(new ZipEntry(tileFilename));
					processRaster(pointcloud, type, tile_rect.xmin, tile_rect.ymin, tile_rect.xmax, tile_rect.ymax, res, fill, tileFormat, receiver);
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			zipOutputStream.finish();
			zipOutputStream.flush();
		} else {
			Receiver receiver = new ResponseReceiver(response);
			processRaster(pointcloud, type, req_xmin, req_ymin, req_xmax, req_ymax, res, fill, format, receiver);
		}
	}

	private void processRaster(PointCloud pointcloud, String type, double req_xmin, double req_ymin, double req_xmax, double req_ymax, double res, int fill, String format, Receiver receiver) throws IOException {
		double proc_xmin = req_xmin;
		double proc_ymin = req_ymin;
		double proc_xmax_excluding = req_xmin + Math.floor((req_xmax - req_xmin) / res) * res + res;
		double proc_ymax_excluding = req_ymin + Math.floor((req_ymax - req_ymin) / res) * res + res;
		double proc_xmax = Math.nextDown(proc_xmax_excluding);
		double proc_ymax = Math.nextDown(proc_ymax_excluding);
		log.info("proc "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);

		int width = 0;
		int height = 0;
		int[][] int_grid = null;
		double[][] double_grid = null;

		switch(type) {
		case "point_count": {
			AttributeSelector selector = new AttributeSelector().setXY();
			Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector);
			PointCountRaster pointCountRaster = new PointCountRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
			pointTables.sequential().forEach(pointCountRaster::insert);
			int_grid = pointCountRaster.grid;
			width = int_grid[0].length;
			height = int_grid.length;
			break;
		}
		case "pulse_count": {
			AttributeSelector selector = new AttributeSelector().setXY().setReturnNumber();
			Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterFirstReturn);
			PointCountRaster pointCountRaster = new PointCountRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
			pointTables.sequential().forEach(pointCountRaster::insert);
			int_grid = pointCountRaster.grid;
			width = int_grid[0].length;
			height = int_grid.length;
			break;
		}
		case "dsm": {
			AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
			Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterEntity);
			PointRaster pointRaster_dsm = new PointRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
			pointTables.sequential().forEach(pointRaster_dsm::insert);
			double[][] grid_dsm = pointRaster_dsm.getTop();
			width = grid_dsm[0].length;
			height = grid_dsm.length;
			if(fill > 0) {
				double[][] dst_dsm = ProcessingDouble.copy(grid_dsm, width, height);
				ProcessorNode_gap_filling.fill(grid_dsm, dst_dsm, width, height, fill);
				grid_dsm = dst_dsm;
			}
			double_grid = grid_dsm;
			break;
		}
		case "dtm": {			
			double_grid = generateDTM(pointcloud, proc_xmin, proc_ymin, proc_xmax, proc_ymax, res, fill);
			width = double_grid[0].length;
			height = double_grid.length;
			break;
		}
		case "chm": {
			AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
			Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterEntity);
			PointRaster pointRaster_dsm = new PointRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
			PointRaster pointRaster_dtm = new PointRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
			pointTables.sequential().forEach(pointTable -> {
				pointRaster_dsm.insert(pointTable);
				pointRaster_dtm.insert(pointTable, pointTable.filterGround());	
			});
			double[][] grid_dsm = pointRaster_dsm.getTop();
			double[][] grid_dtm = pointRaster_dtm.getMedian();
			width = grid_dsm[0].length;
			height = grid_dsm.length;
			if(fill > 0) {
				double[][] dst_dsm = ProcessingDouble.copy(grid_dsm, width, height);
				ProcessorNode_gap_filling.fill(grid_dsm, dst_dsm, width, height, fill);
				grid_dsm = dst_dsm;
				double[][] dst_dtm = ProcessingDouble.copy(grid_dtm, width, height);
				ProcessorNode_gap_filling.fill(grid_dtm, dst_dtm, width, height, fill);
				grid_dtm = dst_dtm;
			}
			double_grid = ProcessingDouble.minus(grid_dsm, grid_dtm, width, height);
			break;
		}
		default:
			throw new RuntimeException("unknown type: " + type);
		}

		switch(format) {
		case "rdat": {
			RdatList meta = new RdatList();
			meta.addString("source", "pointcloud " + pointcloud.getName());
			RdatList bandMeta = new RdatList();
			bandMeta.addString("name", type);
			//TODO check proc_xmax_excluding, proc_ymax_excluding
			RdatWriter rdatWriter = new RdatWriter(width, height, proc_xmin, proc_ymin, proc_xmax_excluding, proc_ymax_excluding, meta);
			if(pointcloud.hasProj4()) {
				rdatWriter.setProj4(pointcloud.getProj4());
			}
			if(int_grid != null) {
				rdatWriter.addRdatBand(RdatBand.ofInt32(width, height, bandMeta, int_grid));
			}
			if(double_grid != null) {
				rdatWriter.addRdatBand(RdatBand.ofFloat64(width, height, bandMeta, double_grid));
			}
			receiver.setStatus(HttpServletResponse.SC_OK);
			receiver.setContentType("application/octet-stream");
			rdatWriter.write(new DataOutputStream(receiver.getOutputStream()));
			break;
		}
		case "js": {
			if(int_grid != null) {
				JsWriter.writeFloat2d(int_grid, receiver);
			}
			if(double_grid != null) {
				JsWriter.writeFloat2d(double_grid, receiver);
			}
			break;
		}
		case "tiff": {
			short epsg = pointcloud.getEPSGcode();
			TiffWriter tiffWriter = new TiffWriter(width, height, proc_xmin, proc_ymin, res, res, epsg);
			if(int_grid != null) {
				tiffWriter.addTiffBand(TiffBand.ofInt32(int_grid, type));
			}
			if(double_grid != null) {
				tiffWriter.addTiffBand(TiffBand.ofFloat64(double_grid, type));
			}
			receiver.setStatus(HttpServletResponse.SC_OK);
			receiver.setContentType("image/tiff");
			receiver.setContentLength(tiffWriter.exactSizeOfWriteAuto());
			tiffWriter.writeAuto(new DataOutputStream(receiver.getOutputStream()));
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}		
	}
	
	public static double[][] generateDTM(PointCloud pointcloud, double proc_xmin, double proc_ymin, double proc_xmax, double proc_ymax, double res, int fill) {
		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterGround);
		PointRaster pointRaster_dtm = new PointRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
		pointTables.sequential().forEach(pointRaster_dtm::insert);
		double[][] grid_dtm = pointRaster_dtm.getMedian();
		int width = grid_dtm[0].length;
		int height = grid_dtm.length;
		if(fill > 0) {
			double[][] dst_dtm = ProcessingDouble.copy(grid_dtm, width, height);
			ProcessorNode_gap_filling.fill(grid_dtm, dst_dtm, width, height, fill);
			grid_dtm = dst_dtm;
		}
		return grid_dtm;
	}
}
