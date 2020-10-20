package server.api.voxeldbs;

import java.io.IOException;

import org.eclipse.jetty.server.Response;

import broker.TimeSlice;
import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.CellFactory.VoxelMapperInt32;
import voxeldb.VoxelCell;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public abstract class AggregatedProcessing {

	public static void process(VoxelDB voxeldb, Range3d range, TimeSlice timeSlice, int aggregation_factor, boolean crop, Response response, String format) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();
		double aggOriginX = ref.voxelXtoGeo(range.xmin);
		double aggOriginY = ref.voxelYtoGeo(range.ymin);
		double aggOriginZ = ref.voxelZtoGeo(range.zmin);
		double aggVoxelSizeX = ref.voxelSizeX * aggregation_factor;
		double aggVoxelSizeY = ref.voxelSizeY * aggregation_factor;
		double aggVoxelSizeZ = ref.voxelSizeZ * aggregation_factor;
		VoxelGeoRef aggRef = ref.with(aggOriginX, aggOriginY, aggOriginZ, aggVoxelSizeX, aggVoxelSizeY, aggVoxelSizeZ);

		String attribute = "count";
		String data_type = "int32";
		String aggregate = "sum";

		CellFactory cellFactory = new CellFactory(voxeldb);	
		VoxelMapperInt32 mapper = cellFactory.registerMapper(attribute);
		AggregatedProcessing aggregatedProcessing = null;
		
		switch(data_type) {
		case "int32": {
			AggregatorInt32Int32 aggregator = null;
			switch(aggregate) {
			case "sum": {
				aggregator = new AggregatorInt32Int32Sum();
				break;
			}
			case "count": {
				aggregator = new AggregatorInt32Int32Count();
				break;
			}
			case "exist": {
				aggregator = new AggregatorInt32Int32Exist();
				break;
			}
			default:
				throw new RuntimeException("unknown aggregate: " + aggregate);
			}
			aggregatedProcessing = new AggregatedProcessingInt32Int32MappedAggregator(cellFactory, range, aggregation_factor, aggRef, mapper, aggregator);
			break;			
		}
		case "bool8": {
			AggregatorInt32Bool8Exist aggregator = null;
			switch(aggregate) {
			case "exist": {
				aggregator = new AggregatorInt32Bool8Exist();
				break;
			}
			default:
				throw new RuntimeException("unknown aggregate: " + aggregate);
			}
			aggregatedProcessing = new AggregatedProcessingInt32Bool8MappedAggregator(cellFactory, range, aggregation_factor, aggRef, mapper, aggregator);
			break;			
		}
		default:
			throw new RuntimeException("unknown data_type: " + data_type);
		}
		cellFactory.getVoxelCells(timeSlice, range).forEach(aggregatedProcessing::apply);
		aggregatedProcessing.write(response, format, crop);		
	}

	protected CellFactory cellFactory;
	protected Range3d range;
	protected int aggregation_factor;
	protected VoxelGeoRef aggRef;	
	protected int xAggLen;
	protected int yAggLen;
	protected int zAggLen;

	public AggregatedProcessing(CellFactory cellFactory, Range3d range, int aggregation_factor, VoxelGeoRef aggRef) {
		this.cellFactory = cellFactory;
		this.cellFactory.setCount();
		this.aggregation_factor = aggregation_factor;
		this.aggRef = aggRef;
		this.range = range;

		this.xAggLen = (range.xmax - range.xmin) / aggregation_factor + 1;
		this.yAggLen = (range.ymax - range.ymin) / aggregation_factor + 1;
		this.zAggLen = (range.zmax - range.zmin) / aggregation_factor + 1;
	}

	public void apply(VoxelCell voxelCell) {
		Range3d srcRange = cellFactory.toRange(voxelCell);
		Range3d srcCpy = srcRange.overlapping(range);		
		int xSrcStart = srcCpy.xmin - srcRange.xmin;
		int ySrcStart = srcCpy.ymin - srcRange.ymin;
		int zSrcStart = srcCpy.zmin - srcRange.zmin;		
		int xSrcEnd = srcCpy.xmax - srcRange.xmin;
		int ySrcEnd = srcCpy.ymax - srcRange.ymin;
		int zSrcEnd = srcCpy.zmax - srcRange.zmin;		
		int xSrcDstOffeset = srcRange.xmin - range.xmin;
		int ySrcDstOffeset = srcRange.ymin - range.ymin;
		int zSrcDstOffeset = srcRange.zmin - range.zmin;
		process(voxelCell, xSrcStart, ySrcStart, zSrcStart, xSrcEnd, ySrcEnd, zSrcEnd, xSrcDstOffeset, ySrcDstOffeset, zSrcDstOffeset);
	}

	public abstract void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset);
	public abstract void write(Response response, String format, boolean crop) throws IOException;	
}
