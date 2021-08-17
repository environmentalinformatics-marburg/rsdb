package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Response;

import broker.TimeSlice;
import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;
import voxeldb.voxelcellprocessors.VcpCntUint16;
import voxeldb.voxelcellprocessors.VcpCntUint8;
import voxeldb.voxelcellprocessors.VcpInt32;

public class VoxelProcessor {
	static final Logger log = LogManager.getLogger();

	private final VoxelDB voxeldb;
	private CellFactory cellFactory;

	public VoxelProcessor(VoxelDB voxeldb, CellFactory cellFactory) {
		this.voxeldb = voxeldb;
		this.cellFactory = cellFactory;
	}

	public void ProcessUint8(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, TimeSlice timeSlice, Response response, String format, VcpCntUint8 vcp) throws IOException {
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		byte[][][] r = new byte[vrzlen][vrylen][vrxlen];

		vcp.setTarget(r);
		cellFactory.getVoxelCells(timeSlice).forEach(vcp);

		Range3d localRange = VoxelProcessing.getRange(r, vrxlen, vrylen, vrzlen);
		if(localRange.isEmpty()) {
			localRange = Range3d.ZERO;
		}
		byte[] data = VoxelProcessing.toBytes(r, localRange);

		Range3d voxelRange = localRange.add(vrxmin, vrymin, vrzmin);

		VoxelWriter.writeUint8(data, voxeldb.getName(), voxeldb.geoRef(), voxelRange, response, format);		
	}
	
	public void ProcessUint16(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, TimeSlice timeSlice, Response response, String format, VcpCntUint16 vcp) throws IOException {
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		char[][][] r = new char[vrzlen][vrylen][vrxlen];

		vcp.setTarget(r);
		cellFactory.getVoxelCells(timeSlice).forEach(vcp);

		Range3d localRange = VoxelProcessing.getRange(r, vrxlen, vrylen, vrzlen);
		if(localRange.isEmpty()) {
			localRange = Range3d.ZERO;
		}
		byte[] data = VoxelProcessing.toBytes(r, localRange);

		Range3d voxelRange = localRange.add(vrxmin, vrymin, vrzmin);

		VoxelWriter.writeUint16(data, voxeldb, voxelRange, response, format);		
	}
	
	public void ProcessInt32(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, TimeSlice timeSlice, Response response, String format, VcpInt32 vcp) throws IOException {
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		int[][][] r = new int[vrzlen][vrylen][vrxlen];

		vcp.setTarget(r);
		cellFactory.getVoxelCells(timeSlice).forEach(vcp);

		Range3d localRange = VoxelProcessing.getRange(r, vrxlen, vrylen, vrzlen);
		if(localRange.isEmpty()) {
			localRange = Range3d.ZERO;
		}
		byte[] data = VoxelProcessing.toBytes(r, localRange);

		Range3d voxelRange = localRange.add(vrxmin, vrymin, vrzmin);

		VoxelWriter.writeInt32(data, voxeldb.getName(), voxeldb.geoRef(), voxelRange, response, format);		
	}
}