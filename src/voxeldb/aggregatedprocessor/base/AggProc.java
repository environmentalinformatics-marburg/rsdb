package voxeldb.aggregatedprocessor.base;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Response;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelGeoRef;

public abstract class AggProc implements Consumer<VoxelCell>{
	private static final Logger log = LogManager.getLogger();
	
	protected CellFactory cellFactory;
	protected Range3d range;
	protected int aggregation_factor_x;
	protected int aggregation_factor_y;
	protected int aggregation_factor_z;
	protected VoxelGeoRef aggRef;	
	protected int xAggLen;
	protected int yAggLen;
	protected int zAggLen;

	public AggProc(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef) {
		this.cellFactory = cellFactory;
		this.cellFactory.setCount();
		this.aggregation_factor_x = aggregation_factor_x;
		this.aggregation_factor_y = aggregation_factor_y;
		this.aggregation_factor_z = aggregation_factor_z;
		this.aggRef = aggRef;
		this.range = range;

		this.xAggLen = (range.xmax - range.xmin) / aggregation_factor_x + 1;
		this.yAggLen = (range.ymax - range.ymin) / aggregation_factor_y + 1;
		this.zAggLen = (range.zmax - range.zmin) / aggregation_factor_z + 1;
	}

	@Override
	public void accept(VoxelCell voxelCell) {
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
	
	public void finish() {		
	}

	public abstract void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset);
	public abstract void write(Response response, String format, boolean crop) throws IOException;	
}
