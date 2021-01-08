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
		
		boolean crop = Web.getFlagBoolean(request, "crop");

		AggregatedProcessing.process(voxeldb, range, timeSlice, aggregation_factor, crop, response, format);		
	}
}
