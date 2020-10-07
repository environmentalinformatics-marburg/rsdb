package server.api.pointdb;

import java.io.IOException;
import java.util.Arrays;

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
import util.Util;
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
public class APIHandler_chm extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_chm(Broker broker) {
		super(broker, "chm");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);		
		PointDB pointdb = getPointdb(request);
		Rect requestRect = Rect.of_extent_request(request);
		Rect rect = requestRect.withBorderUTM(PointGrid.window_size).outerMeterRect();
		log.info("rect "+rect);
		//Filter filter = Filter.createFilter("last_return=1");
		PointFilter filter = null;

		String[] columns = null;
		String columnsText = request.getParameter("columns");
		if(columnsText!=null) {
			columns = Util.columnTextToColumns(columnsText, false);
			log.info("columns: "+ Arrays.toString(columns));
		}

		Vec<GeoPoint> rawPoints = pointdb.tilePointProducer(rect).filter(filter).toGeoPointProducer().toList();
		RasterGrid rasterGrid_DTM = DTM_generator.generate(rawPoints, rect);		
		RasterGrid rasterGrid_DSM = DSM_generator.generate(rawPoints, rect);		
		RasterGrid rasterGrid_CHM = rasterGrid_DSM;		
		rasterGrid_CHM.minus_zero(rasterGrid_DTM);		
		rasterGrid_CHM.meta.put("name", "chm");		
		RdatRaster.write_RDAT_RASTER(new ResponseReceiver(response), rasterGrid_CHM, pointdb.config.getProj4());

	}
}
