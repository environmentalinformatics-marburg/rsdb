package server.api.pointclouds;

import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import broker.Broker;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.CellTable.ChainedFilterFunc;
import pointcloud.PointCloud;
import pointdb.base.Rect;
import pointdb.subsetdsl.Region;
import server.api.pointclouds.PointProcessor.PointTableTransformFunc;
import util.Receiver;
import util.ResponseReceiver;
import util.StreamReceiver;
import util.Web;

public class APIHandler_points {
	private static final Logger log = LogManager.getLogger();

	protected static final String MIME_JSON = "application/json";	
	protected static final String MIME_CSV = "text/csv";

	//private final Broker broker;

	public APIHandler_points(Broker broker) {
		//this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, HttpServletResponse response) throws IOException {
		String extText = request.getParameter("ext");
		String polygonText = request.getParameter("polygon");
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;

		Rect boundingRect = null;
		Region requestRegion = null;

		if(polygonText != null) {
			if(extText != null) {
				throw new RuntimeException("If 'polygon' parameter is present, parameter 'ext' can not be specified.");
			}
			String[] cTexts = polygonText.split(" ");
			int len = cTexts.length;
			if(len % 2 != 0) {
				throw new RuntimeException();
			}
			if(len < 6) {
				throw new RuntimeException();
			}
			int vlen = len / 2;
			double[] vx = new double[vlen];
			double[] vy = new double[vlen];
			for (int i = 0; i < vlen; i++) {
				vx[i] = Double.parseDouble(cTexts[i]);
				if(vx[i]<xmin) xmin = vx[i];
				if(vx[i]>xmax) xmax = vx[i];
			}
			for (int i = 0; i < vlen; i++) {
				vy[i] = Double.parseDouble(cTexts[i + vlen]);
				if(vy[i]<ymin) ymin = vy[i];
				if(vy[i]>ymax) ymax = vy[i];
			}
			requestRegion = Region.ofPolygon(vx, vy);
			boundingRect = requestRegion.bbox;
		} else if(extText != null) {
			String[] ext = extText.split(" ");
			if(ext.length != 4) {
				throw new RuntimeException("parameter error in 'ext': "+extText);
			}
			xmin = Double.parseDouble(ext[0]);
			ymin = Double.parseDouble(ext[1]);
			xmax = Double.parseDouble(ext[2]);
			ymax = Double.parseDouble(ext[3]);
			boundingRect = Rect.of_UTM(xmin, ymin, xmax, ymax);
			requestRegion = Region.ofRect(boundingRect);
		} else {
			throw new RuntimeException("missing parameter 'ext' (or 'polygon')");
		}
		
		PointTableTransformFunc transformFunc = null;
		/*boolean normalise_ground = Web.getBoolean(request, "normalise_ground", false);
		if(normalise_ground) {
			transformFunc = Normalise::normalise_ground;
		}*/
		PointTableTransformFunc tf = transformFunc;


		String columnsText = request.getParameter("columns");
		AttributeSelector selector = columnsText == null ? null : AttributeSelector.of(columnsText.split("\\s+"));
		ChainedFilterFunc filterFunc = CellTable.parseFilter(request.getParameter("filter"));

		if(format.equals("zip")) {
			String tileFormat = "las";
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			Receiver receiver = new StreamReceiver(zipOutputStream);
			Region rr = requestRegion;
			boundingRect.tiles_utmm(1000_000, 1000_000, (xtile, ytile, tileRect) -> {
				log.info(tileRect);
				String tileFilename = "tile_" + xtile + "_" + ytile + ".las";
				try {
					Region tileRegion = Region.ofFilteredBbox(tileRect, rr.polygonPoints);
					zipOutputStream.putNextEntry(new ZipEntry(tileFilename));
					double txmin = tileRect.getUTMd_min_x();
					double tymin = tileRect.getUTMd_min_y();
					double txmax = tileRect.getUTMd_max_x_inclusive();
					double tymax = tileRect.getUTMd_max_y_inclusive();
					PointProcessor.process(pointcloud, txmin, tymin, txmax, tymax, tf, filterFunc, tileRegion, tileFormat, receiver, request, selector);
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}				
			});			
			zipOutputStream.finish();
			zipOutputStream.flush();
		} else {
			ResponseReceiver receiver = new ResponseReceiver(response);		
			PointProcessor.process(pointcloud, xmin, ymin, xmax, ymax, transformFunc, filterFunc, requestRegion, format, receiver, request, selector);
		}
	}
}
