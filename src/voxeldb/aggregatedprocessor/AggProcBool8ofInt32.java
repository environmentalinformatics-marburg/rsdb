package voxeldb.aggregatedprocessor;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelGeoRef;
import voxeldb.aggregatedprocessor.base.AggProcBool8;
import voxeldb.aggregator.base.AggBool8ofInt32;
import voxeldb.voxelmapper.VoxelMapperInt32;

public class AggProcBool8ofInt32 extends AggProcBool8 {

	private VoxelMapperInt32 mapper;
	private AggBool8ofInt32 aggregator;

	public AggProcBool8ofInt32(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef, VoxelMapperInt32 mapper, AggBool8ofInt32 aggregator) {
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef);
		this.mapper = mapper;
		this.aggregator = aggregator;
	}
	
	public void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset) {
		int[][][] src = mapper.map(voxelCell);
		aggregator.process(src, xSrcStart, ySrcStart, zSrcStart, xSrcEnd, ySrcEnd, zSrcEnd, dst, xSrcDstOffset, ySrcDstOffset, zSrcDstOffset, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z);		
	}
}
