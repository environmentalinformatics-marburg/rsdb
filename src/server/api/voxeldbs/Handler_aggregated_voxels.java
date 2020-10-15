package server.api.voxeldbs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.TimeSlice;
import util.Range3d;
import util.Web;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

public class Handler_aggregated_voxels {
	private static final Logger log = LogManager.getLogger();

	public void handle(VoxelDB voxeldb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();

		String format = Web.getString(request, "format");

		String extText = request.getParameter("ext");
		if(extText == null) {
			throw new RuntimeException("missing extent: parameter 'ext'");
		}	
		String[] ext = extText.split(" ");
		if(ext.length != 6) {
			throw new RuntimeException("parameter error in 'ext': "+extText);
		}
		double geoXmin = Double.parseDouble(ext[0]);
		double geoYmin = Double.parseDouble(ext[1]);
		double geoZmin = Double.parseDouble(ext[2]);
		double geoXmax = Double.parseDouble(ext[3]);
		double geoYmax = Double.parseDouble(ext[4]);
		double geoZmax = Double.parseDouble(ext[5]);
		Range3d range = ref.geoToRange(geoXmin, geoYmin, geoZmin, geoXmax, geoYmax, geoZmax);
		int aggregation_factor = Web.getInt(request, "aggregation_factor");

		TimeSlice timeSlice;
		if(Web.has(request, "time_slice_id")) {
			int time_slice_id = Web.getInt(request, "time_slice_id");
			timeSlice = voxeldb.timeMapReadonly.get(time_slice_id);
			if(timeSlice == null) {
				throw new RuntimeException("uknown time_slice_id: " + time_slice_id);
			}
			if(Web.has(request, "time_slice_name") && !Web.getString(request, "time_slice_name").equals(timeSlice.name)) {
				throw new RuntimeException("time_slice_name does not match to time slice of time_slice_id: '" + Web.getString(request, "time_slice_name") + "'  '" + timeSlice.name + "'");
			}
		} else if(Web.has(request, "time_slice_name")) {
			String time_slice_name = Web.getString(request, "time_slice_name");
			timeSlice = voxeldb.getTimeSliceByName(time_slice_name);
			if(timeSlice == null) {
				throw new RuntimeException("unknown time_slice_name: " + time_slice_name);
			}
		} else {
			if(voxeldb.timeMapReadonly.isEmpty()) {
				throw new RuntimeException("no data");
			}
			timeSlice = voxeldb.timeMapReadonly.lastEntry().getValue();
		}

		int xAggLen = (range.xmax - range.xmin) / aggregation_factor + 1;
		int yAggLen = (range.ymax - range.ymin) / aggregation_factor + 1;
		int zAggLen = (range.zmax - range.zmin) / aggregation_factor + 1;
		int[][][] dst = new int[zAggLen][yAggLen][xAggLen];
		
		CellFactory cellFactory = new CellFactory(voxeldb).setCount();
		cellFactory.getVoxelCells(timeSlice, range).forEach(voxelCell -> {
			Range3d srcRange = cellFactory.toRange(voxelCell);
			if(voxelCell.cnt != null) {
				aggregateSum(voxelCell.cnt, srcRange, dst, range, aggregation_factor);
			}
		});

		Range3d aggRange = new Range3d(0, 0, 0, (range.xmax - range.xmin) / aggregation_factor, (range.ymax - range.ymin) / aggregation_factor, (range.zmax - range.zmin) / aggregation_factor);
		byte[] data = VoxelProcessing.toBytes(dst, xAggLen, yAggLen, zAggLen);
		double aggOriginX = ref.voxelXtoGeo(range.xmin);
		double aggOriginY = ref.voxelYtoGeo(range.ymin);
		double aggOriginZ = ref.voxelZtoGeo(range.zmin);
		double aggVoxelSizeX = ref.voxelSizeX * aggregation_factor;
		double aggVoxelSizeY = ref.voxelSizeY * aggregation_factor;
		double aggVoxelSizeZ = ref.voxelSizeZ * aggregation_factor;
		VoxelGeoRef aggRef = ref.with(aggOriginX, aggOriginY, aggOriginZ, aggVoxelSizeX, aggVoxelSizeY, aggVoxelSizeZ);
		VoxelWriter.writeInt32(data, voxeldb.getName(), aggRef, aggRange, response, format);		
	}

	public void aggregateSum(int[][][] src, Range3d srcRange, int[][][] dst, Range3d srcDstRange, int factor) {		
		Range3d srcCpy = srcRange.overlapping(srcDstRange);		
		int xSrcStart = srcCpy.xmin - srcRange.xmin;
		int ySrcStart = srcCpy.ymin - srcRange.ymin;
		int zSrcStart = srcCpy.zmin - srcRange.zmin;		
		int xSrcEnd = srcCpy.xmax - srcRange.xmin;
		int ySrcEnd = srcCpy.ymax - srcRange.ymin;
		int zSrcEnd = srcCpy.zmax - srcRange.zmin;		
		int xSrcDstOffeset = srcRange.xmin - srcDstRange.xmin;
		int ySrcDstOffeset = srcRange.ymin - srcDstRange.ymin;
		int zSrcDstOffeset = srcRange.zmin - srcDstRange.zmin;
		for(int z = zSrcStart; z <= zSrcEnd; z++) {
			int[][] srcZ = src[z];
			int[][] dstZ = dst[(z + zSrcDstOffeset) / factor];
			for(int y = ySrcStart; y <= ySrcEnd; y++) {
				int[] srcZY = srcZ[y];
				int[] dstZY = dstZ[(y + ySrcDstOffeset) / factor];
				for(int x = xSrcStart; x <= xSrcEnd; x++) {
					dstZY[(x + xSrcDstOffeset) / factor] += srcZY[x];
				}
			}
		}
	}
}
