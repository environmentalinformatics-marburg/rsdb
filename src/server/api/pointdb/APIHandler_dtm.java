package server.api.pointdb;

import java.io.IOException;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.PointGrid;
import pointdb.processing.geopoint.RasterGrid;
import pointdb.processing.tilepoint.PointFilter;
import util.ResponseReceiver;
import util.Timer;
import util.collections.vec.Vec;
import util.rdat.RdatRaster;

/**
 * digital terrain model (DTM)
 * @author woellauer
 *
 */
@Deprecated
public class APIHandler_dtm extends PointdbAPIHandler {
	

	public APIHandler_dtm(Broker broker) {
		super(broker, "dtm");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);		
		PointDB pointdb = getPointdb(request);
		Rect rect = Rect.of_extent_request(request);
		rect = rect.withBorderUTM(PointGrid.window_size).outerMeterRect();
		Logger.info("rect "+rect);
		PointFilter filter = PointFilter.createFilter("last_return=1");

		Vec<GeoPoint> rawPoints = pointdb.tilePointProducer(rect).filter(filter).toGeoPointProducer().toList();		
		Timer.start("dtm");
		RasterGrid rasterGrid = null;
		//for(int i=0;i<10;i++) {
			rasterGrid = DTM_generator.generate(rawPoints, rect);
		//}
		Logger.info(Timer.stop("dtm"));
		rasterGrid.meta.put("name", "dtm");
		RdatRaster.write_RDAT_RASTER(new ResponseReceiver(response), rasterGrid, pointdb.config.getProj4());

	}
}
