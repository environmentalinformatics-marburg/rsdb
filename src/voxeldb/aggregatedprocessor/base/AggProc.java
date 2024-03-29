package voxeldb.aggregatedprocessor.base;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCellProc;
import voxeldb.VoxelGeoRef;

public abstract class AggProc extends VoxelCellProc {

	protected final int aggregation_factor_x;
	protected final int aggregation_factor_y;
	protected final int aggregation_factor_z;
	protected final VoxelGeoRef aggRef;	
	protected final int xAggLen;
	protected final int yAggLen;
	protected final int zAggLen;

	public AggProc(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef) {
		super(cellFactory, range);
		this.aggregation_factor_x = aggregation_factor_x;
		this.aggregation_factor_y = aggregation_factor_y;
		this.aggregation_factor_z = aggregation_factor_z;
		this.aggRef = aggRef;

		this.xAggLen = (range.xmax - range.xmin) / aggregation_factor_x + 1;
		this.yAggLen = (range.ymax - range.ymin) / aggregation_factor_y + 1;
		this.zAggLen = (range.zmax - range.zmin) / aggregation_factor_z + 1;
	}
}
