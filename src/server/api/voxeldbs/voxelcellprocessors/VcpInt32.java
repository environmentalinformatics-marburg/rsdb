package server.api.voxeldbs.voxelcellprocessors;

public abstract class VcpInt32 extends Vcp {
	protected int[][][] r;

	public VcpInt32(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize) {
		super(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
	}
	
	public void setTarget(int[][][] target) {
		this.r = target;
	}	
}