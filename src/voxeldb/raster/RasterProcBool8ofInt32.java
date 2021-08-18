package voxeldb.raster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelGeoRef;
import voxeldb.raster.agg.RasterAggBool8ofInt32;
import voxeldb.voxelmapper.VoxelMapperInt32;

public class RasterProcBool8ofInt32 extends RasterProcBool8 {
	static final Logger log = LogManager.getLogger();
	
	private final VoxelMapperInt32 mapper;
	private final RasterAggBool8ofInt32 aggregator;

	public RasterProcBool8ofInt32(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, VoxelGeoRef aggRef, VoxelMapperInt32 mapper, RasterAggBool8ofInt32 aggregator) {	
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggRef);
		this.mapper = mapper;
		this.aggregator = aggregator;
	}

	@Override
	public void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset) {
		int[][][] src = mapper.map(voxelCell);
		aggregator.process(src, xSrcStart, ySrcStart, zSrcStart, xSrcEnd, ySrcEnd, zSrcEnd, dst, xSrcDstOffset, ySrcDstOffset, zSrcDstOffset, aggregation_factor_x, aggregation_factor_y);		
	}
}
