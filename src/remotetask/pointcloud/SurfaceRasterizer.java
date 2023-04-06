package remotetask.pointcloud;

import java.io.IOException;
import java.util.stream.Stream;

import broker.TimeSlice;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointcloud.TopFloatRaster;
import pointcloud.ZRasterFloat;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.node.ProcessorNode_gap_filling;
import rasterdb.tile.ProcessingFloat;
import rasterunit.RasterUnitStorage;
import remotetask.MessageProxy;
import util.TaskPipeline.PipelineTask;

public class SurfaceRasterizer extends PipelineTask {

	private final MessageProxy messageProxy;
	private final PointCloud pointcloud; 
	private final RasterDB rasterdb; 
	private final TimeSlice timeSlice; 
	private final Band band; 
	private final int raster_xmin; 
	private final int raster_ymin; 
	private final int raster_xmax; 
	private final int raster_ymax; 
	private final boolean commit; 
	private final int fill; 
	private final String rasterType;

	private float[][] raster_result = null;

	public SurfaceRasterizer(MessageProxy messageProxy, PointCloud pointcloud, RasterDB rasterdb, TimeSlice timeSlice, Band band, int raster_xmin, int raster_ymin, int raster_xmax, int raster_ymax, boolean commit, int fill, String rasterType) {
		this.messageProxy = messageProxy;
		this.pointcloud = pointcloud;
		this.rasterdb = rasterdb;
		this.timeSlice = timeSlice;
		this.band = band;
		this.raster_xmin = raster_xmin;
		this.raster_ymin = raster_ymin;
		this.raster_xmax = raster_xmax;
		this.raster_ymax = raster_ymax;
		this.commit = commit;
		this.fill = fill;
		this.rasterType = rasterType;
	}

	protected final void setMessage(String message) {
		messageProxy.setMessage(message);
	}

	protected final void log(String message) {
		messageProxy.log(message);
	}

	@Override
	protected void process() {
		try {
			processRaster();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	protected void finish() {
		if(raster_result != null) {
			try {
				RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
				int cnt = ProcessingFloat.writeMerge(rasterUnit, timeSlice.id, band, raster_result, raster_ymin, raster_xmin);
				raster_result = null;
				if(commit) {
					rasterUnit.commit();
					setMessage("commited");
				}
				setMessage("tiles written: " + cnt);		
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}	
	}

	private void processRaster() throws IOException {
		switch(rasterType) {
		case "DSM":
			processRasterDSM();
			break;
		case "DTM":
			processRasterDTM();
			break;	
		case "CHM":
			processRasterCHM();
			break;				
		default:
			throw new RuntimeException("unknown raster type: " + rasterType);
		}
	}

	private void processRasterDSM() throws IOException {
		GeoReference ref = rasterdb.ref();
		ref.throwNotSameXYpixelsize();
		double res = ref.pixel_size_x;
		double proc_xmin = ref.pixelXToGeo(raster_xmin - fill);
		double proc_ymin = ref.pixelYToGeo(raster_ymin - fill);
		double proc_xmax = ref.pixelXToGeoUpper(raster_xmax + fill);
		double proc_ymax = ref.pixelYToGeoUpper(raster_ymax + fill);
		int raster_width = raster_xmax - raster_xmin + 1;
		int raster_height = raster_ymax - raster_ymin + 1;
		int proc_raster_width = raster_width + fill + fill;
		int proc_raster_height = raster_height + fill + fill;
		setMessage("process raster "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);
		TopFloatRaster topFloatRaster_DSM = new TopFloatRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);

		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterEntity);
		pointTables.sequential().forEach(topFloatRaster_DSM::insert);
		if(topFloatRaster_DSM.getInsertedPointTablesCount() > 0) {
			float[][] raster_DSM = topFloatRaster_DSM.grid;
			if(fill > 0) {
				float[][] raster_DSM_dst = ProcessingFloat.copy(raster_DSM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DSM, raster_DSM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DSM = ProcessingFloat.withoutBorder(raster_DSM_dst, fill);			
			}

			raster_result = raster_DSM;
		} else {
			setMessage("skip empty");
			removeFromFinishQueue();
		}
	}

	private void processRasterDTM() throws IOException {
		GeoReference ref = rasterdb.ref();
		ref.throwNotSameXYpixelsize();
		double res = ref.pixel_size_x;
		double proc_xmin = ref.pixelXToGeo(raster_xmin - fill);
		double proc_ymin = ref.pixelYToGeo(raster_ymin - fill);
		double proc_xmax = ref.pixelXToGeoUpper(raster_xmax + fill);
		double proc_ymax = ref.pixelYToGeoUpper(raster_ymax + fill);
		int raster_width = raster_xmax - raster_xmin + 1;
		int raster_height = raster_ymax - raster_ymin + 1;
		int proc_raster_width = raster_width + fill + fill;
		int proc_raster_height = raster_height + fill + fill;
		setMessage("process raster "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);
		ZRasterFloat zRasterFloat_DTM = new ZRasterFloat(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);

		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterGround);
		pointTables.sequential().forEach(zRasterFloat_DTM::insert);
		if(zRasterFloat_DTM.getInsertedPointTablesCount() > 0) {
			float[][] raster_DTM = zRasterFloat_DTM.getMedian();
			if(fill > 0) {
				float[][] raster_DTM_dst = ProcessingFloat.copy(raster_DTM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DTM, raster_DTM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DTM = ProcessingFloat.withoutBorder(raster_DTM_dst, fill);			
			}

			raster_result = raster_DTM;
		} else {
			setMessage("skip empty");
			removeFromFinishQueue();
		}
	}

	private void processRasterCHM() throws IOException {
		GeoReference ref = rasterdb.ref();
		ref.throwNotSameXYpixelsize();
		double res = ref.pixel_size_x;
		double proc_xmin = ref.pixelXToGeo(raster_xmin - fill);
		double proc_ymin = ref.pixelYToGeo(raster_ymin - fill);
		double proc_xmax = ref.pixelXToGeoUpper(raster_xmax + fill);
		double proc_ymax = ref.pixelYToGeoUpper(raster_ymax + fill);
		int raster_width = raster_xmax - raster_xmin + 1;
		int raster_height = raster_ymax - raster_ymin + 1;
		int proc_raster_width = raster_width + fill + fill;
		int proc_raster_height = raster_height + fill + fill;
		setMessage("process raster "+proc_xmin+" "+proc_ymin+" "+proc_xmax+" "+proc_ymax);
		TopFloatRaster topFloatRaster_DSM = new TopFloatRaster(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);
		ZRasterFloat zRasterFloat_DTM = new ZRasterFloat(proc_xmin, proc_ymin, proc_xmax, proc_ymax, res);

		AttributeSelector selector = new AttributeSelector().setXYZ().setClassification();
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, proc_xmin, proc_ymin, proc_xmax, proc_ymax, selector, CellTable::filterEntity);	
		pointTables.sequential().forEach(pointTable -> {
			topFloatRaster_DSM.insert(pointTable);
			zRasterFloat_DTM.insert(pointTable, pointTable.filterGround());	
		});		
		if(topFloatRaster_DSM.getInsertedPointTablesCount() > 0) {
			float[][] raster_DSM = topFloatRaster_DSM.grid;
			float[][] raster_DTM = zRasterFloat_DTM.getMedian();
			if(fill > 0) {
				float[][] raster_DSM_dst = ProcessingFloat.copy(raster_DSM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DSM, raster_DSM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DSM = ProcessingFloat.withoutBorder(raster_DSM_dst, fill);

				float[][] raster_DTM_dst = ProcessingFloat.copy(raster_DTM, proc_raster_width, proc_raster_height);
				ProcessorNode_gap_filling.fillBordered(raster_DTM, raster_DTM_dst, proc_raster_width, proc_raster_height, fill);
				raster_DTM = ProcessingFloat.withoutBorder(raster_DTM_dst, fill);			
			}
			float[][] raster_CHM = ProcessingFloat.minus(raster_DSM, raster_DTM, raster_DSM[0].length, raster_DSM.length);

			raster_result = raster_CHM;
		} else {
			setMessage("skip empty");
			removeFromFinishQueue();
		}		
	}	
}
