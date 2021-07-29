package voxeldb.aggregatedprocessor.base;

import java.io.IOException;

import org.eclipse.jetty.server.Response;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelGeoRef;
import server.api.voxeldbs.VoxelProcessing;
import server.api.voxeldbs.VoxelWriter;

public abstract class AggProcBool8 extends AggProc {

	protected boolean[][][] dst;

	public AggProcBool8(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, VoxelGeoRef aggRef) {
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef);
		this.cellFactory.setCount();
		dst = new boolean[zAggLen][yAggLen][xAggLen];	
	}
	
	public void write(Response response, String format, boolean crop) throws IOException {
		Range3d aggRange = null;
		byte[] data = null;
		if(crop) {
			aggRange = VoxelProcessing.getRange(dst, xAggLen, yAggLen, zAggLen);
			data = VoxelProcessing.toBytes(dst, aggRange);
		} else {
			aggRange = new Range3d(0, 0, 0, (range.xmax - range.xmin) / aggregation_factor_x, (range.ymax - range.ymin) / aggregation_factor_y, (range.zmax - range.zmin) / aggregation_factor_z);
			data = VoxelProcessing.toBytes(dst, xAggLen, yAggLen, zAggLen);			
		}		
		VoxelWriter.writeBool8(data, cellFactory.getvoxeldb().getName(), aggRef, aggRange, response, format);		
	}
}
