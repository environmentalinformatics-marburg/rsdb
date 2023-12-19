package server.api.pointdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.Point2d;
import pointdb.base.Rect;
import pointdb.processing.geopoint.GeoPointFilter;
import pointdb.processing.geopoint.Normalise;
import pointdb.subsetdsl.Region;
import util.Receiver;
import util.ResponseReceiver;
import util.StreamReceiver;
import util.Timer;
import util.Util;
import util.Web;

public class APIHandler_query extends PointdbAPIHandler {
	
	
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

	public APIHandler_query(Broker broker) {
		super(broker, "query");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Timer.start("query points");
		PointDB pointdb = getPointdb(request);

		if(request.getParameter("polygon") != null && request.getParameter("ext") != null) {
			throw new RuntimeException("only one parameter allowed: polygon or ext");
		}

		Rect boundingRect = null;
		Region requestRegion = null;
		
		if(request.getParameter("polygon") != null) {
			String pText = request.getParameter("polygon");
			Point2d[] polygon = parsePolygon(pText);
			requestRegion = Region.ofPlainPolygon(polygon);
			boundingRect = requestRegion.bbox;
		} else if(request.getParameter("ext") != null) {
			double[] requestExtent = Web.getDoubles(request, "ext");
			Logger.info("doubles "+Arrays.toString(requestExtent));
			if(requestExtent.length!=4) {
				throw new RuntimeException("parameter ext needs four values: "+Arrays.toString(requestExtent));
			}
			double x1 = requestExtent[0];
			double x2 = requestExtent[1];
			double y1 = requestExtent[2];		
			double y2 = requestExtent[3];
			double xmin = x1<x2?x1:x2;
			double ymin = y1<y2?y1:y2;
			double xmax = x1<x2?x2:x1;
			double ymax = y1<y2?y2:y1;
			boundingRect = Rect.of_UTM(xmin, ymin, xmax, ymax);
			requestRegion = Region.ofRect(boundingRect);
		} else {
			throw new RuntimeException("one parameter needed: polygon or ext");
		}

		Normalise normalise = Normalise.parse(request.getParameter("normalise"));

		String format = Web.getString(request, "format", "rdat");

		GeoPointFilter filter0 = null;
		String filterText = request.getParameter("filter");
		if(filterText!=null) {
			filter0 = GeoPointFilter.createFilter(filterText);
			Logger.info("filter: "+filterText+" "+filter0);
		}
		final GeoPointFilter filter = filter0;

		String[] columns0 = null;
		String columnsText = request.getParameter("columns");
		if(columnsText!=null) {
			columns0 = Util.columnTextToColumns(columnsText, false);
			Logger.info("columns: "+ Arrays.toString(columns0));
		}
		final String[] columns = columns0;

		boolean sort0 = false; // always sort if true else sort if needed for other processing (e.g. normalise extremes, ground)
		switch(Web.getString(request, "sort", "no")) {
		case "z":
			Logger.info("sorting");
			sort0 = true;
			break;
		case "no":
			//nothing
			break;
		default:
			Logger.warn("unknown sort parameter "+Web.getString(request, "sort", "no"));
		}
		final boolean sort = sort0;



		double xmin = 0;
		double ymin = 0;
		if(format.equals("zip")) {			
			String tileFormat = "las";
			response.setContentType(Web.MIME_ZIP);
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			Receiver receiver = new StreamReceiver(zipOutputStream);
			Region rr = requestRegion;
			boundingRect.tiles_utmm(1000_000, 1000_000, (xtile, ytile, tileRect) -> {
				Logger.info(tileRect);
				String tileFilename = "tile_" + xtile + "_" + ytile + ".las";
				try {
					Region tileRegion = Region.ofFilteredBbox(tileRect, rr.polygons);
					zipOutputStream.putNextEntry(new ZipEntry(tileFilename));
					PointProcessor.process(pointdb, tileRegion, normalise, filter, sort, columns, tileFormat, -xmin, -ymin, receiver, Long.MAX_VALUE);
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}				
			});			
			zipOutputStream.finish();
			zipOutputStream.flush();
		} else {
			Receiver receiver = new ResponseReceiver(response);
			PointProcessor.process(pointdb, requestRegion, normalise, filter, sort, columns, format, -xmin, -ymin, receiver, Long.MAX_VALUE);
		}
		Logger.info(Timer.stop("query points"));
	}
}
