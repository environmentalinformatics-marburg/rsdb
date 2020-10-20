package server.api.voxeldbs;

import java.io.IOException;

import org.eclipse.jetty.server.Response;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelGeoRef;

public abstract class AggregatedProcessingBool8 extends AggregatedProcessing {

	protected boolean[][][] dst;

	public AggregatedProcessingBool8(CellFactory cellFactory, Range3d range, int aggregation_factor, VoxelGeoRef aggRef) {
		super(cellFactory, range, aggregation_factor, aggRef);
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
			aggRange = new Range3d(0, 0, 0, (range.xmax - range.xmin) / aggregation_factor, (range.ymax - range.ymin) / aggregation_factor, (range.zmax - range.zmin) / aggregation_factor);
			data = VoxelProcessing.toBytes(dst, xAggLen, yAggLen, zAggLen);			
		}		
		VoxelWriter.writeBool8(data, cellFactory.getvoxeldb().getName(), aggRef, aggRange, response, format);		
	}
}
