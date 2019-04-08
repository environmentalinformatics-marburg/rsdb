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
import pointdb.base.Point2d;
import pointdb.base.Rect;
import pointdb.processing.geopoint.GeoPointProducer;
import pointdb.processing.geopoint.Normalise;
import pointdb.processing.tilepoint.PointFilter;
import util.Util;
import util.collections.vec.Vec;
import util.rdat.RdatPointDataFrame;

public class APIHandler_polygon extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();
	
	public APIHandler_polygon(Broker broker) {
		super(broker, "polygon");
	}
	
	private static Point2d[] parsePolygon(String pText) {
		String[] pTextArray = pText.split("__");
		if(pTextArray.length<3) {
			throw new RuntimeException("polygon needs at least three different points");
		}
		Point2d[] polygon = new Point2d[pTextArray.length+1];
		for (int i = 0; i<pTextArray.length; i++) {
			String[] cord = pTextArray[i].split("_");
			if(cord.length!=2) {
				throw new RuntimeException("any point of polygon needs two coordinate values");
			}
			double x = Double.parseDouble(cord[0]);
			double y = Double.parseDouble(cord[1]);
			polygon[i] = Point2d.of(x, y);
		}
		polygon[polygon.length-1] = polygon[0];
		return polygon;
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		log.info("get polygon");
		request.setHandled(true);
		
		PointDB pointdb = getPointdb(request);
		String pText = request.getParameter("polygon");
		if(pText==null) {
			throw new RuntimeException("missing parameter: polygon");
		}
		Point2d[] polygon = parsePolygon(pText);
		
		PointFilter filter = null;
		String filterText = request.getParameter("filter");
		if(filterText!=null) {
			filter = PointFilter.createFilter(filterText);
			log.info("filter: "+filterText+" "+filter);
		}
		
		String[] columns = null;
		String columnsText = request.getParameter("columns");
		if(columnsText!=null) {
			columns = Util.columnTextToColumns(columnsText);
			log.info("columns: "+ Arrays.toString(columns));
		}
		
		Normalise normalise = Normalise.parse(request.getParameter("normalise"));

		Rect rect = Rect.of_polygon(polygon);
		log.info(rect);
		GeoPointProducer producer = pointdb.tilePointProducer(rect).filter(filter).toGeoPointProducer().clip(polygon);
		if(normalise.normalise_origin) {
			producer = producer.transform(-rect.getUTMd_min_x(), -rect.getUTMd_min_y());
		}
		Vec<GeoPoint> result = normalise.optional_normalise(producer.toList());
		log.info("points "+result.size());
		RdatPointDataFrame.writePointList(pointdb, response,result,columns);
	}
}
