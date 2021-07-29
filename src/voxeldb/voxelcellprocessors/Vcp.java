package voxeldb.voxelcellprocessors;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import voxeldb.VoxelCell;

public abstract class Vcp implements Consumer<VoxelCell> {	
	private static final Logger log = LogManager.getLogger();
	
	protected final int vrxmin;
	protected final int vrymin;
	protected final int vrzmin;
	protected final int vrxmax;
	protected final int vrymax;
	protected final int vrzmax;
	protected final int cellsize;

	public Vcp(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize) {
		this.vrxmin = vrxmin;
		this.vrymin = vrymin;
		this.vrzmin = vrzmin;
		this.vrxmax = vrxmax;
		this.vrymax = vrymax;
		this.vrzmax = vrzmax;
		this.cellsize = cellsize;
	}

	@Override
	public final void accept(VoxelCell voxelCell) {
		int vcxmin = voxelCell.x * cellsize;
		int vcymin = voxelCell.y * cellsize;
		int vczmin = voxelCell.z * cellsize;
		int vcxmax = vcxmin + cellsize - 1;
		int vcymax = vcymin + cellsize - 1;
		int vczmax = vczmin + cellsize - 1;			
		//log.info(voxelCell);
		//log.info("vc "+vcxmin+" "+vcymin+" "+vczmin+"   "+vcxmax+" "+vcymax+" "+vczmax);

		int vbxmin = Math.max(vrxmin, vcxmin);
		int vbymin = Math.max(vrymin, vcymin);
		int vbzmin = Math.max(vrzmin, vczmin);
		int vbxmax = Math.min(vrxmax, vcxmax);
		int vbymax = Math.min(vrymax, vcymax);
		int vbzmax = Math.min(vrzmax, vczmax);

		process(voxelCell, vbxmin, vbymin, vbzmin, vbxmax, vbymax, vbzmax, vcxmin, vcymin, vczmin);		
	}
	
	protected abstract void process(VoxelCell voxelCell, int vbxmin, int vbymin, int vbzmin, int vbxmax, int vbymax, int vbzmax, int vcxmin, int vcymin, int vczmin);
}