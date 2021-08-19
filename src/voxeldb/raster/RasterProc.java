package voxeldb.raster;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCellProc;
import voxeldb.VoxelGeoRef;

public abstract class RasterProc extends VoxelCellProc {

	protected final int aggregation_factor_x;
	protected final int aggregation_factor_y;
	protected final VoxelGeoRef aggRef;
	protected final int xAggLen;
	protected final int yAggLen;
	protected final Range3d aggRange;

	public RasterProc(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, VoxelGeoRef aggRef) {
		super(cellFactory, range);
		this.aggregation_factor_x = aggregation_factor_x;
		this.aggregation_factor_y = aggregation_factor_y;
		this.aggRef = aggRef;
		this.xAggLen = (range.xmax - range.xmin) / aggregation_factor_x + 1;
		this.yAggLen = (range.ymax - range.ymin) / aggregation_factor_y + 1;
		this.aggRange = new Range3d(0, 0, 0, xAggLen - 1, yAggLen - 1, 0);		
	}
}
