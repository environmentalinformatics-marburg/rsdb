package server.api.pointclouds;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Request;
import org.tinylog.Logger;

import com.googlecode.javaewah.datastructure.BitSet;

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
import util.Web;

public class PointProcessor {

	@FunctionalInterface
	public static interface PointTableTransformFunc extends Function<PointTable, PointTable>{
	}
	
	private static int getPrecision(PointCloud pointcloud) {
		double cellscale = pointcloud.getCellscale();
		double prec = Math.ceil(Math.log10(cellscale));
		return 0 <= prec && 6 <= prec ? (prec <= 0 ? 0 : (int) prec) : 2;
	}

	public static void process(PointCloud pointcloud, int t, double xmin, double ymin, double xmax, double ymax, PointTableTransformFunc transformFunc, ChainedFilterFunc filterFunc, Region region, String file_format, Receiver receiver, Request request, AttributeSelector selector, String[] columns, boolean useRawPoints, long limit) throws IOException {
		String data_format = request.getParameter("format");

		Logger.info("process points  data format: " + data_format);

		if(useRawPoints || "xzy_rgb".equals(data_format)) { // processings: just polygon filter,  workaround for xzy_rgb to include RGB in processing
			Logger.info("useRawPoints");

			AttributeSelector queryAttributeSelector = selector == null ? new AttributeSelector(true) : selector;
			//Logger.info("queryAttributeSelector " + queryAttributeSelector); 
			Stream<PointTable> pointTables = pointcloud.getPointTables(t, xmin, ymin, xmax, ymax, queryAttributeSelector, filterFunc);

			if(transformFunc != null) {
				pointTables.map(transformFunc);
			}

			if(!region.isBbox()) {
				Function<PointTable, BitSet> filterByPolygonFunc = PointTable.getFilterByPolygonsWithHolesFunc(region.polygons);
				pointTables = pointTables.map(pointTable -> PointTable.applyMask(pointTable, filterByPolygonFunc.apply(pointTable)));
			}

			switch(file_format) {
			case "xyz": {
				if(limit == Long.MAX_VALUE) {
					PointTableWriter.writeXYZ(pointTables, receiver);
				} else {
					PointTableWriter.writeXYZ(pointTables, receiver, limit);
				}
				break;
			}
			case "csv": {
				String[] columnNames = columns;
				if(columnNames == null) {
					AttributeSelector attributeSelector = pointcloud.getSelector();
					columnNames = attributeSelector.toArray();
				}
				String separator = Web.getString(request, "separator", ",");
				if("tab".equals(separator)) {
					separator = "\t";
				}
				int precision = getPrecision(pointcloud);
				PointTableWriter.writeCSV(pointTables, receiver, columnNames, separator, precision, limit);
				break;
			}
			case "las": {
				PointTableWriter.writeLAS(pointTables.toArray(PointTable[]::new), receiver);
				break;
			}
			case "js": {
				PointTableWriter.writeJs(pointTables.toArray(PointTable[]::new), data_format, receiver);
				break;
			}
			case "rdat": {
				PointTableWriter.writeRdat(pointTables.toArray(PointTable[]::new), receiver, selector);
				break;
			}
			default:
				throw new RuntimeException("unknown format: "+file_format);
			}
		} else {			
			DataProvider2 dp2 = new DataProvider2(pointcloud, t, region);
			Normalise normalise = Normalise.parse(request.getParameter("normalise"));
			GeoPointFilter filter = null;
			String filterText = request.getParameter("filter");
			if(filterText!=null) {
				filter = GeoPointFilter.createFilter(filterText);
				Logger.info("filter: "+filterText+" "+filter);
			}
			boolean sort = false; // always sort if true else sort if needed for other processing (e.g. normalise extremes, ground)
			switch(Web.getString(request, "sort", "no")) {
			case "z":
				Logger.info("sorting");
				sort = true;
				break;
			case "no":
				//nothing
				break;
			default:
				Logger.warn("unknown sort parameter "+Web.getString(request, "sort", "no"));
			}
			double xnorm = 0;
			double ynorm = 0;
			String proj4 = pointcloud.getProj4();
			server.api.pointdb.PointProcessor.process(dp2, normalise, filter, sort, columns, file_format, xnorm, ynorm, proj4, receiver, limit);
		}
	}	
}
