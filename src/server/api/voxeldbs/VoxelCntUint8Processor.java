package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Response;

import broker.TimeSlice;
import util.Range3d;
import voxeldb.VoxelDB;

public class VoxelCntUint8Processor {
	static final Logger log = LogManager.getLogger();

	private final VoxelDB voxeldb;

	public VoxelCntUint8Processor(VoxelDB voxeldb) {
		this.voxeldb = voxeldb;
	}

	public void Process(int vrxmin, int vrymin, int vrzmin, int vrxmax, int vrymax, int vrzmax, TimeSlice timeSlice, Response response, String format) throws IOException {
		int vrxlen = vrxmax - vrxmin + 1;
		int vrylen = vrymax - vrymin + 1; 
		int vrzlen = vrzmax - vrzmin + 1;
		byte[][][] r = new byte[vrzlen][vrylen][vrxlen];

		int cellsize = voxeldb.getCellsize();
		
		voxeldb.getVoxelCells(timeSlice).forEach(new VoxelCellProcessorCntUint8(vrxmin, vrymin, vrzmin, vrxmax, vrymax, vrzmax, cellsize, r));

		Range3d localRange = VoxelProcessing.getRange(r, vrxlen, vrylen, vrzlen);
		if(localRange.isEmpty()) {
			localRange = Range3d.ZERO;
		}
		byte[] data = VoxelProcessing.toBytes(r, localRange);

		Range3d voxelRange = localRange.add(vrxmin, vrymin, vrzmin);

		VoxelWriter.write(data, voxeldb, voxelRange, response, format);		
	}	
}