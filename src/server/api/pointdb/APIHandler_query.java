package server.api.pointdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import util.Receiver;
import util.ResponseReceiver;
import util.StreamReceiver;
import util.Timer;
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
		Timer.start("query points");
		PointDB pointdb = getPointdb(request);

		double[] ext = Web.getDoubles(request, "ext");
		log.info("doubles "+Arrays.toString(ext));
		if(ext.length!=4) {
			throw new RuntimeException("parameter ext needs four values: "+Arrays.toString(ext));
		}

		Normalise normalise = Normalise.parse(request.getParameter("normalise"));

		String format = Web.getString(request, "format", "rdat");

		GeoPointFilter filter0 = null;
		String filterText = request.getParameter("filter");
		if(filterText!=null) {
			filter0 = GeoPointFilter.createFilter(filterText);
			log.info("filter: "+filterText+" "+filter0);
		}
		final GeoPointFilter filter = filter0;
		
		String[] columns0 = null;
		String columnsText = request.getParameter("columns");
		if(columnsText!=null) {
			columns0 = Util.columnTextToColumns(columnsText);
			log.info("columns: "+ Arrays.toString(columns0));
		}
		final String[] columns = columns0;

		boolean sort0 = false; // always sort if true else sort if needed for other processing (e.g. normalise extremes, ground)
		switch(Web.getString(request, "sort", "no")) {
		case "z":
			log.info("sorting");
			sort0 = true;
			break;
		case "no":
			//nothing
			break;
		default:
			log.warn("unknown sort parameter "+Web.getString(request, "sort", "no"));
		}
		final boolean sort = sort0;
		
		double x1 = ext[0];
		double x2 = ext[1];
		double y1 = ext[2];		
		double y2 = ext[3];
		double xmin = x1<x2?x1:x2;
		double ymin = y1<y2?y1:y2;
		double xmax = x1<x2?x2:x1;
		double ymax = y1<y2?y2:y1;
		Rect rect = Rect.of_UTM(xmin, ymin, xmax, ymax);
		
		if(format.equals("zip")) {			
			String tileFormat = "las";
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			Receiver receiver = new StreamReceiver(zipOutputStream);
			rect.tiles_utmm(1000_000, 1000_000, (xtile, ytile, tileRect) -> {
				log.info(tileRect);
				String tileFilename = "tile_" + xtile + "_" + ytile + ".las";
				try {
					zipOutputStream.putNextEntry(new ZipEntry(tileFilename));
					PointProcessor.process(pointdb, tileRect, normalise, filter, sort, columns, tileFormat, -xmin, -ymin, receiver);
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}				
			});			
			zipOutputStream.finish();
			zipOutputStream.flush();
		} else {
			Receiver receiver = new ResponseReceiver(response);
			PointProcessor.process(pointdb, rect, normalise, filter, sort, columns, format, -xmin, -ymin, receiver);
		}
		log.info(Timer.stop("query points"));
	}
}
