package voxeldb;

import java.io.IOException;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Response;

import util.Range3d;

public abstract class VoxelCellProc implements Consumer<VoxelCell> {
	
	protected final CellFactory cellFactory;
	protected final Range3d range;

	public VoxelCellProc(CellFactory cellFactory, Range3d range) {
		this.cellFactory = cellFactory;
		this.cellFactory.setCount();
		this.range = range;
	}

	@Override
	public final void accept(VoxelCell voxelCell) {
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

	public abstract void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffset, int ySrcDstOffset, int zSrcDstOffset);
	public abstract void write(Response response, String format, boolean crop) throws IOException;	
}
