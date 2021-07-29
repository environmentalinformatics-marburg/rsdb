package voxeldb.cellproc;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Response;

import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;

public abstract class CellProc implements Consumer<VoxelCell>{
	private static final Logger log = LogManager.getLogger();
	
	protected CellFactory cellFactory;
	protected Range3d range;	
	protected int xLen;
	protected int yLen;
	protected int zLen;

	public CellProc(CellFactory cellFactory, Range3d range) {
		this.cellFactory = cellFactory;
		this.cellFactory.setCount();
		this.range = range;
		this.xLen = range.xlen();
		this.yLen = range.ylen();
		this.zLen = range.zlen();
	}

	@Override
	public void accept(VoxelCell voxelCell) {
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

	public abstract void process(VoxelCell voxelCell, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset);
	public abstract void write(Response response, String format, boolean crop) throws IOException;	
}
