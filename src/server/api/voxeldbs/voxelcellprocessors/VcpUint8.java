package server.api.voxeldbs.voxelcellprocessors;

public abstract class VcpUint8 extends Vcp {
	protected byte[][][] r;

	public VcpUint8(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, int cellsize) {
		super(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize);
	}
	
	public void setTarget(byte[][][] target) {
		this.r = target;
	}	
}