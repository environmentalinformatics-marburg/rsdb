package server.api.pointclouds;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

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
import rasterdb.ProcessingDouble;
import rasterdb.node.ProcessorNode_gap_filling;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;

public class APIHandler_raster {
	private static final Logger log = LogManager.getLogger();

	//private final Broker broker;

	private final static double SMALL_VALUE = 0.000001d;

	public static final String[][] RASTER_TYPES = new String[][] {
		{"point_count", "count of points per pixel"},
		{"pulse_count", "count of laser puleses per pixel"},
		{"dsm", "Digital Surface Model"},
		{"dtm", "Digital Terrain Model"},
		{"chm", "Canopy Height Model"},
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
		double res = 1;
		if(resText != null) {
			res = Double.parseDouble(resText);
		}

		String fillText = request.getParameter("fill");
		int fill = 0;
		if(fillText != null) {
			fill = Integer.parseInt(fillText);
			if(fill < 0 || fill > 100) {
				throw new RuntimeException("fill value out of range (0 to 100): " + fill);
			}
		}

		double res_xmin = req_xmin;
		double res_ymin = req_ymin;
		double res_xmax = Math.floor((req_xmax - req_xmin) / res) * res + res_xmin;
		double res_ymax = Math.floor((req_ymax - req_ymin) / res) * res + res_ymin;
		log.info("res "+res_xmin+" "+res_ymin+" "+res_xmax+" "+res_ymax);

		double proc_add = res - SMALL_VALUE;
		double proc_xmin = res_xmin;
		double proc_ymin = res_ymin;
		double proc_xmax = res_xmax + proc_add;
		double proc_ymax = res_ymax + proc_add;
		log.info("proc "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);

		String typeText = request.getParameter("type");
		String type = "count";
		if(typeText != null) {
			type = typeText;
		}

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
			AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
			Stream<PointTable> pointTables = pointcloud.getPointTables(proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterGround);
			PointRaster pointRaster_dtm = new PointRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
			pointTables.sequential().forEach(pointRaster_dtm::insert);
			double[][] grid_dtm = pointRaster_dtm.getMedian();
			width = grid_dtm[0].length;
			height = grid_dtm.length;
			if(fill > 0) {
				double[][] dst_dtm = ProcessingDouble.copy(grid_dtm, width, height);
				ProcessorNode_gap_filling.fill(grid_dtm, dst_dtm, width, height, fill);
				grid_dtm = dst_dtm;
			}
			double_grid = grid_dtm;
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
			RdatWriter rdatWriter = new RdatWriter(width, height, res_xmin, res_ymin, res_xmax + res, res_ymax + res, meta);
			if(pointcloud.hasProj4()) {
				rdatWriter.setProj4(pointcloud.getProj4());
			}
			if(int_grid != null) {
				rdatWriter.addRdatBand(RdatBand.ofInt32(width, height, bandMeta, int_grid));
			}
			if(double_grid != null) {
				rdatWriter.addRdatBand(RdatBand.ofFloat64(width, height, bandMeta, double_grid));
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/octet-stream");
			rdatWriter.write(new DataOutputStream(response.getOutputStream()));
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}
	}



}
