package voxeldb.aggregatedprocessor.base;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelGeoRef;

public class AggProcFloat32Delegate2Div extends AggProcFloat32Delegate2 {

	public AggProcFloat32Delegate2Div(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef, AggProcFloat32 aggProcA, AggProcFloat32 aggProcB) {
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, aggProcA, aggProcB);
	}

	@Override
	public void finish() {
		float[][][] srcA = aggProcA.dst;
		float[][][] srcB = aggProcB.dst;
		for (int z = 0; z < zAggLen; z++) {
			float[][] srcAZ = srcA[z];
			float[][] srcBZ = srcB[z];
			float[][] dstZ = dst[z];
			for (int y = 0; y < yAggLen; y++) {
				float[] srcAZY = srcAZ[y];
				float[] srcBZY = srcBZ[y];
				float[] dstZY = dstZ[y];
				for (int x = 0; x < xAggLen; x++) {
					dstZY[x] = srcAZY[x] / srcBZY[x];
				}
			}
		}
	}
}
