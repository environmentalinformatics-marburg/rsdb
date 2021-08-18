package voxeldb.aggregatedprocessor;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelGeoRef;
import voxeldb.aggregatedprocessor.base.AggProcFloat32;
import voxeldb.aggregator.base.AggFloat32ofInt32;
import voxeldb.voxelmapper.VoxelMapperInt32;

public class AggProcFloat32ofInt32 extends AggProcFloat32 {

	private final VoxelMapperInt32 mapper;
	private final AggFloat32ofInt32 aggregator;

	public AggProcFloat32ofInt32(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef, VoxelMapperInt32 mapper, AggFloat32ofInt32 aggregator) {
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef);
		this.mapper = mapper;
		this.aggregator = aggregator;
	}
	
	public void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset) {
		int[][][] srcA = mapper.map(voxelCell);
		aggregator.process(srcA, xSrcStart, ySrcStart, zSrcStart, xSrcEnd, ySrcEnd, zSrcEnd, dst, xSrcDstOffset, ySrcDstOffset, zSrcDstOffset, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z);		
	}
}
