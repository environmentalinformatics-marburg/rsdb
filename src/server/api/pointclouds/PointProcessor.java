package server.api.pointclouds;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import pointcloud.AttributeSelector;
import pointcloud.CellTable.ChainedFilterFunc;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointcloud.PointTable.FilterByPolygonFunc;
import pointdb.process.DataProvider2;
import pointdb.processing.geopoint.GeoPointFilter;
import pointdb.processing.geopoint.Normalise;
import pointdb.subsetdsl.Region;
import util.Receiver;
import util.Util;
import util.Web;

public class PointProcessor {
	private static final Logger log = LogManager.getLogger();

	@FunctionalInterface
	public static interface PointTableTransformFunc extends Function<PointTable, PointTable>{
	}

	public static void process(PointCloud pointcloud, int t, double xmin, double ymin, double xmax, double ymax, PointTableTransformFunc transformFunc, ChainedFilterFunc filterFunc, Region region, String format, Receiver receiver, Request request, AttributeSelector selector) throws IOException {
		boolean useRawPoints = false;

		if(useRawPoints) { // processings: just polygon filter
			log.info("useRawPoints");

			AttributeSelector queryAttributeSelector = selector == null ? new AttributeSelector(true) : selector;
			//log.info("queryAttributeSelector " + queryAttributeSelector); 
			Stream<PointTable> pointTables = pointcloud.getPointTables(t, xmin, ymin, xmax, ymax, queryAttributeSelector, filterFunc);

			if(transformFunc != null) {
				pointTables.map(transformFunc);
			}

			if(!region.isBbox()) {
				FilterByPolygonFunc filterByPolygonFunc = PointTable.getFilterByPolygonFunc(region.polygonPoints);
				pointTables = pointTables.map(pointTable -> PointTable.applyMask(pointTable, filterByPolygonFunc.apply(pointTable)));
			}

			switch(format) {
			case "xyz": {
				PointTableWriter.writeXYZ(pointTables, receiver);
				break;
			}
			case "las": {
				PointTableWriter.writeLAS(pointTables.toArray(PointTable[]::new), receiver);
				break;
			}
			case "js": {
				PointTableWriter.writeJS(pointTables.toArray(PointTable[]::new), request, receiver);
				break;
			}
			case "rdat": {
				PointTableWriter.writeRdat(pointTables.toArray(PointTable[]::new), receiver, selector);
				break;
			}
			default:
				throw new RuntimeException("unknown format: "+format);
			}
		} else {			
			DataProvider2 dp2 = new DataProvider2(pointcloud, t, region);
			Normalise normalise = Normalise.parse(request.getParameter("normalise"));
			GeoPointFilter filter = null;
			String filterText = request.getParameter("filter");
			if(filterText!=null) {
				filter = GeoPointFilter.createFilter(filterText);
				log.info("filter: "+filterText+" "+filter);
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
			String[] columns = null;
			String columnsText = request.getParameter("columns");
			if(columnsText!=null) {
				columns = Util.columnTextToColumns(columnsText, true);
				log.info("columns: "+ Arrays.toString(columns));
			}
			double xnorm = 0;
			double ynorm = 0;
			String proj4 = pointcloud.getProj4();
			server.api.pointdb.PointProcessor.process(dp2, normalise, filter, sort, columns, format, xnorm, ynorm, proj4, receiver);
		}
	}	
}
