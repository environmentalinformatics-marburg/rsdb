package voxeldb.raster;

import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.jetty.server.Response;

import jakarta.servlet.http.HttpServletResponse;
import util.Extent3d;
import util.Range3d;
import util.Web;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;
import voxeldb.CellFactory;
import voxeldb.VoxelGeoRef;

public abstract class RasterProcInt32 extends RasterProc {
	
	protected int[][] dst;

	public RasterProcInt32(CellFactory cellFactory, Range3d range, int aggregation_factor_x, int aggregation_factor_y, VoxelGeoRef aggRef) {
		super(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggRef);
		dst = new int[yAggLen][xAggLen];	
	}

	@Override
	public void write(Response response, String format, boolean crop) throws IOException {		
		switch(format) {
		case "rdat": {
			RdatList meta = new RdatList();
			RdatList bandMeta = new RdatList();
			Extent3d aggGeoExtent = aggRef.toGeoExtent(aggRange);
			RdatWriter rdatWriter = new RdatWriter(xAggLen, yAggLen, aggGeoExtent.xmin, aggGeoExtent.ymin, aggGeoExtent.xmax, aggGeoExtent.ymax, meta);
			if(aggRef.hasProj4()) {
				rdatWriter.setProj4(aggRef.proj4);
			}
			rdatWriter.addRdatBand(RdatBand.ofInt32(xAggLen, yAggLen, bandMeta, dst));
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_BINARY);
			rdatWriter.write(new DataOutputStream(response.getOutputStream()));
			break;
		}
		default:
			throw new RuntimeException("unknown format: " + format);
		}	
	}
}
