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
import pointdb.process.DataProvider2;
import pointdb.processing.geopoint.GeoPointFilter;
import pointdb.processing.geopoint.Normalise;
import pointdb.subsetdsl.Region;
import server.api.pointdb.LasWriter.LAS_HEADER;
import server.api.pointdb.LasWriter.POINT_DATA_RECORD;
import util.Util;
import util.Web;
import util.collections.vec.Vec;
import util.rdat.RdatPointDataFrame;

public class APIHandler_query extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_query(Broker broker) {
		super(broker, "query");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		PointDB pointdb = getPointdb(request);

		double[] ext = Web.getDoubles(request, "ext");
		log.info("doubles "+Arrays.toString(ext));
		if(ext.length!=4) {
			throw new RuntimeException("parameter ext needs four values: "+Arrays.toString(ext));
		}

		Normalise normalise = Normalise.parse(request.getParameter("normalise"));

		String format = Web.getString(request, "format", "rdat");

		GeoPointFilter filter = null;
		String filterText = request.getParameter("filter");
		if(filterText!=null) {
			filter = GeoPointFilter.createFilter(filterText);
			log.info("filter: "+filterText+" "+filter);
		}

		String[] columns = null;
		String columnsText = request.getParameter("columns");
		if(columnsText!=null) {
			columns = Util.columnTextToColumns(columnsText);
			log.info("columns: "+ Arrays.toString(columns));
		}

		boolean sort = false; // always sort if true else sort if needed for other processing (e.g. normalise extremes, ground)
		switch(Web.getString(request, "sort", "no")) {
		case "z":
			log.info("sorting");
			sort = true;
			break;
		case "no":
			//nothing
			break;
		default:
			log.warn("unknown sort parameter "+Web.getString(request, "sort", "no"));
		}

		double x1 = ext[0];
		double x2 = ext[1];
		double y1 = ext[2];		
		double y2 = ext[3];
		double xmin = x1<x2?x1:x2;
		double ymin = y1<y2?y1:y2;
		double xmax = x1<x2?x2:x1;
		double ymax = y1<y2?y2:y1;
		Region region = Region.ofRect(Rect.of_UTM(xmin, ymin, xmax, ymax));
		//log.info("query region " + region);
		DataProvider2 dp2 = new DataProvider2(pointdb, region);


		Vec<GeoPoint> points = null;

		if(normalise.normalise_ground) {
			points = dp2.get_sortedRegionHeightPoints();
			log.info("query points " + points.size());
			if(normalise.normalise_extremes) {
				points = Normalise.no_extermes_of_sorted(points);
			}
			if(normalise.normalise_origin) {
				points = Normalise.translate(points, -xmin, -ymin);
			}
		} else {
			points = dp2.get_regionPoints();
			if(normalise.normalise_extremes) {
				points.sort(GeoPoint.Z_COMPARATOR_SAFE);
				points = Normalise.no_extermes_of_sorted(points);
			} else {
				if(sort) {
					points.sort(GeoPoint.Z_COMPARATOR_SAFE);
				}
			}
			if(normalise.normalise_origin) {
				points = Normalise.translate(points, -xmin, -ymin);
			}
		}

		if(filter != null) {
			points = points.filter(filter);
		}		

		switch(format.trim().toLowerCase()) {
		case "rdat":
			RdatPointDataFrame.writePointList(pointdb, response, points, columns);
			break;
		case "js":
			JsWriter.writePoints(pointdb, response, points, columns);
			break;
		case "xyz":
			PointXyzWriter.writePoints(pointdb, response, points, columns);
			break;
		case "las":
			LasWriter.writePoints(pointdb, response, points, columns, LAS_HEADER.V_1_2, POINT_DATA_RECORD.FORMAT_0);
			break;
		default:
			throw new RuntimeException("unknown format "+format);
		}
	}
}
