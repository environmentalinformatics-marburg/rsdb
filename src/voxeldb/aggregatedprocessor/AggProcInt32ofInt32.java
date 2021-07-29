package voxeldb.aggregatedprocessor;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelGeoRef;
import voxeldb.aggregatedprocessor.base.AggProcInt32;
import voxeldb.aggregator.base.AggInt32ofInt32;
import voxeldb.voxelmapper.VoxelMapperInt32;

public class AggProcInt32ofInt32 extends AggProcInt32 {

	private VoxelMapperInt32 mapper;
	private AggInt32ofInt32 aggregator;

	public AggProcInt32ofInt32(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef, VoxelMapperInt32 mapper, AggInt32ofInt32 aggregator) {
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef);
		this.mapper = mapper;
		this.aggregator = aggregator;
	}
	
	public void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset) {
		int[][][] src = mapper.map(voxelCell);
		aggregator.process(src, xSrcStart, ySrcStart, zSrcStart, xSrcEnd, ySrcEnd, zSrcEnd, dst, xSrcDstOffeset, ySrcDstOffeset, zSrcDstOffeset, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z);		
	}
}
