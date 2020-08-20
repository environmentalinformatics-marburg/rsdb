package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Response;

import broker.TimeSlice;
import server.api.voxeldbs.voxelcellprocessors.VcpCntInt32;
import server.api.voxeldbs.voxelcellprocessors.VcpCntUint8;
import util.Range3d;
import voxeldb.VoxelDB;

public class VoxelProcessor {
	static final Logger log = LogManager.getLogger();

	private final VoxelDB voxeldb;

	public VoxelProcessor(VoxelDB voxeldb) {
		this.voxeldb = voxeldb;
	}

	public void ProcessUint8(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, TimeSlice timeSlice, Response response, String format, VcpCntUint8 vcp) throws IOException {
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		byte[][][] r = new byte[vrzlen][vrylen][vrxlen];

		vcp.setTarget(r);
		voxeldb.getVoxelCells(timeSlice).forEach(vcp);

		Range3d localRange = VoxelProcessing.getRange(r, vrxlen, vrylen, vrzlen);
		if(localRange.isEmpty()) {
			localRange = Range3d.ZERO;
		}
		byte[] data = VoxelProcessing.toBytes(r, localRange);

		Range3d voxelRange = localRange.add(vrxmin, vrymin, vrzmin);

		VoxelWriter.writeUint8(data, voxeldb, voxelRange, response, format);		
	}
	
	public void ProcessInt32(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, TimeSlice timeSlice, Response response, String format, VcpCntInt32 vcp) throws IOException {
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		int[][][] r = new int[vrzlen][vrylen][vrxlen];

		vcp.setTarget(r);
		voxeldb.getVoxelCells(timeSlice).forEach(vcp);

		Range3d localRange = VoxelProcessing.getRange(r, vrxlen, vrylen, vrzlen);
		if(localRange.isEmpty()) {
			localRange = Range3d.ZERO;
		}
		byte[] data = VoxelProcessing.toBytes(r, localRange);

		Range3d voxelRange = localRange.add(vrxmin, vrymin, vrzmin);

		VoxelWriter.writeInt32(data, voxeldb, voxelRange, response, format);		
	}
}