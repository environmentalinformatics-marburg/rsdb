package server.api.rasterdb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.TimeSlice;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import pointdb.base.Point2d;
import rasterdb.Band;
import rasterdb.BandProcessing;
import rasterdb.BandProcessor;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.dsl.DSL;
import rasterdb.dsl.ErrorCollector;
import util.Range2d;
import util.TemplateUtil;
import util.TimeUtil;
import util.Web;
import util.collections.vec.Vec;
import util.frame.DoubleFrame;

public class RasterdbMethod_wms_GetFeatureInfo {

	public static void handle_GetFeatureInfo(RasterDB rasterdb, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String bboxParam = request.getParameter("BBOX");
		Rect2d rect2d = null;
		if(bboxParam != null) {
			String[] bbox = bboxParam.split(",");
			rect2d = Rect2d.parseBbox(bbox);
		}		
		int reqWidth = Web.getInt(request, "WIDTH");
		int reqHeight = Web.getInt(request, "HEIGHT");		
		int reqX = Web.getInt(request, "I");
		int reqY = Web.getInt(request, "J");

		double xres = rect2d.width() / reqWidth;
		double yres = rect2d.height() / reqHeight;

		Rect2d pixelRect2d = new Rect2d(rect2d.xmin + xres * reqX, rect2d.ymax - yres * (reqY + 1), rect2d.xmin + xres * (reqX + 1), rect2d.ymax - yres * reqY);

		Logger.info(reqWidth + " " + reqHeight + "   " + reqX + " " + reqY);
		Logger.info(rect2d);
		Logger.info(pixelRect2d);

		String infoFormat = request.getParameter("INFO_FORMAT");
		switch(infoFormat) {
		case "text/plain": 
			handle_GetFeatureInfoTXT(rasterdb, request, response, pixelRect2d);
			break;
		case "text/html": 
		default: 
			handle_GetFeatureInfoHTML(rasterdb, request, response, pixelRect2d);
		}
	}


	private static HashMap<String, Object> valueOf(Band band, double value) {
		HashMap<String, Object> ctx = new HashMap<>();
		ctx.put("band", band);
		ctx.put("value", value);
		return ctx;
	}
	
	private static HashMap<String, Object> createCtx(RasterDB rasterdb, Request request, Response response, Rect2d pixelRect2d) {
		String layers = Web.getString(request, "QUERY_LAYERS", null);
		if(layers == null) {
			layers = Web.getString(request, "LAYERS", null);
		}
		if(layers == null) {
			throw new RuntimeException("no QUERY_LAYERS or LAYERS parameter");
		}
		String[] layerList = layers.split(",", -1);
		if(layerList.length > 1) {
			Logger.warn("multiple layers specified in LAYERS. Using first layer only. " + Arrays.toString(layerList));
		}
		String layer = layerList[0];
		String[] lparams = layer.split("/", -1);
		String bandText = lparams[0];
		int timestamp = lparams.length > 1 ? Integer.parseInt(lparams[1]) : rasterdb.rasterUnit().timeKeysReadonly().isEmpty() ? 0 : rasterdb.rasterUnit().timeKeysReadonly().last();
		if(layerList.length > 2) {
			Logger.warn("Only the two layer parameters 'bandText' or 'bandText/timestamp' are supported. " + Arrays.toString(lparams));
		}
		Range2d pixelRange2d = rasterdb.ref().rect2dToRange2d(pixelRect2d);
		Range2d centerPixelRange2d = pixelRange2d.toCenterPixel();
		BandProcessor processor = new BandProcessor(rasterdb, centerPixelRange2d, timestamp);
		Rect2d centerPixelrect = rasterdb.ref().range2dToRect2d(centerPixelRange2d);		
		Point2d centerPoint = centerPixelrect.toMinPoint();

		HashMap<String, Object> ctx = new HashMap<>();
		
		ctx.put("rasterdb", rasterdb.config.getName());
		
		ctx.put("coordinates", centerPoint);
		ctx.put("processing", bandText);		

		TimeSlice timeSlice = rasterdb.timeMapReadonly.get(timestamp);			
		ctx.put("time_slice", timeSlice == null ? new TimeSlice(timestamp, TimeUtil.toText(timestamp)) : timeSlice);		

		Vec<HashMap<String, Object>> values = new Vec<HashMap<String, Object>>();

		if(bandText.equals("color")) {
			TimeBand[] timeBands = TimeBand.of(timestamp, BandProcessing.getBestColorBands(rasterdb));
			for(TimeBand timeBand : timeBands) {
				DoubleFrame doubleFrame = processor.getDoubleFrame(timeBand);
				Logger.info(doubleFrame);
				values.add(valueOf(timeBand.band, doubleFrame.getFirstValue()));
			}
		} else if(bandText.startsWith("band")) {
			int bandIndex = Integer.parseInt(bandText.substring(4));
			TimeBand timeBand = processor.getTimeBand(bandIndex);
			DoubleFrame doubleFrame = processor.getDoubleFrame(timeBand);
			Logger.info(doubleFrame);
			values.add(valueOf(timeBand.band, doubleFrame.getFirstValue()));
		} else {
			ErrorCollector errorCollector = new ErrorCollector();
			DoubleFrame[] doubleFrames = DSL.process(bandText, errorCollector, rasterdb, processor);	
			for (int i = 0; i < doubleFrames.length; i++) {
				Logger.info(doubleFrames[i]);
				values.add(valueOf(Band.of(-1, i + 1, "Processing result", null), doubleFrames[i].getFirstValue()));
			}

		}

		ctx.put("values", values);
		return ctx;
	}

	private static void handle_GetFeatureInfoHTML(RasterDB rasterdb, Request request, Response response, Rect2d pixelRect2d) throws IOException {
		HashMap<String, Object> ctx = createCtx(rasterdb, request, response, pixelRect2d);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_HTML);
		TemplateUtil.getTemplate("rasterdb_wms_GetFeatureInfo_html.mustache", true).execute(ctx, response.getWriter());		
	}
	
	private static void handle_GetFeatureInfoTXT(RasterDB rasterdb, Request request, Response response, Rect2d pixelRect2d) throws IOException {
		HashMap<String, Object> ctx = createCtx(rasterdb, request, response, pixelRect2d);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_TEXT);
		TemplateUtil.getTemplate("rasterdb_wms_GetFeatureInfo_txt.mustache", true).execute(ctx, response.getWriter());		
	}
}
