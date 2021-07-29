package voxeldb.voxelcellprocessors;

public abstract class VcpUint16 extends Vcp {
	protected char[][][] r;

	public VcpUint16(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize) {
		super(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
	}
	
	public void setTarget(char[][][] target) {
		this.r = target;
	}	
}