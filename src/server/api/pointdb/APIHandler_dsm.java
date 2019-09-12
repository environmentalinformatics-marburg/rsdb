package server.api.pointdb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import util.collections.vec.Vec;
import util.rdat.RdatRaster;

/**
 * digital surface model (DSM)
 * 
 * ( preparing step for canopy height model (CHM) )
 *  
 * @author woellauer
 *
 */
@Deprecated
public class APIHandler_dsm extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_dsm(Broker broker) {
		super(broker, "dsm");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);		
		PointDB pointdb = getPointdb(request);
		Rect rect = Rect.of_extent_request(request);
		rect = rect.withBorderUTM(PointGrid.window_size).outerMeterRect();
		log.info("rect "+rect);
		//Filter filter = Filter.createFilter("last_return=1");
		PointFilter filter = null;

		Vec<GeoPoint> rawPoints = pointdb.tilePointProducer(rect).filter(filter).toGeoPointProducer().toList();

		RasterGrid rasterGrid_DSM = DSM_generator.generate(rawPoints, rect);
		
		
		rasterGrid_DSM.meta.put("name", "dsm");
		RdatRaster.write_RDAT_RASTER(new ResponseReceiver(response), rasterGrid_DSM, pointdb.config.getProj4());

	}
}
