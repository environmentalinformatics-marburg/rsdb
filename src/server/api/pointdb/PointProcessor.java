package server.api.pointdb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.process.DataProvider2;
import pointdb.processing.geopoint.GeoPointFilter;
import pointdb.processing.geopoint.Normalise;
import pointdb.subsetdsl.Region;
import server.api.pointdb.LasWriter.LAS_HEADER;
import server.api.pointdb.LasWriter.POINT_DATA_RECORD;
import util.Receiver;
import util.collections.vec.Vec;
import util.rdat.RdatPointDataFrame;

public class PointProcessor {
	private static final Logger log = LogManager.getLogger();
	
	public static void process(PointDB pointdb, Region region, Normalise normalise, GeoPointFilter filter, boolean sort, String[] columns, String format, double xnorm, double ynorm, Receiver receiver) throws IOException {
		log.info("query region " + region);
		DataProvider2 dp2 = new DataProvider2(pointdb, region);


		Vec<GeoPoint> points = null;

		if(normalise.normalise_ground) {
			points = dp2.get_sortedRegionHeightPoints();
			log.info("query points " + points.size());
			if(normalise.normalise_extremes) {
				points = Normalise.no_extermes_of_sorted(points);
			}
			if(normalise.normalise_origin) {
				points = Normalise.translate(points, xnorm, ynorm);
			}
		} else {
			points = dp2.get_regionPoints(false);
			if(normalise.normalise_extremes) {
				points.sort(GeoPoint.Z_COMPARATOR_SAFE);
				points = Normalise.no_extermes_of_sorted(points);
			} else {
				if(sort) {
					points.sort(GeoPoint.Z_COMPARATOR_SAFE);
				}
			}
			if(normalise.normalise_origin) {
				points = Normalise.translate(points, xnorm, ynorm);
			}
		}

		if(filter != null) {
			points = points.filter(filter);
		}		

		switch(format.trim().toLowerCase()) {
		case "rdat":
			RdatPointDataFrame.writePointList(pointdb, receiver, points, columns);
			break;
		case "js":
			JsWriter.writePoints(pointdb, receiver, points, columns);
			break;
		case "xyz":
			PointXyzWriter.writePoints(pointdb, receiver, points, columns);
			break;
		case "las":
			LasWriter.writePoints(pointdb, receiver, points, columns, LAS_HEADER.V_1_2, POINT_DATA_RECORD.FORMAT_0);
			break;
		default:
			throw new RuntimeException("unknown format "+format);
		}		
	}

}
