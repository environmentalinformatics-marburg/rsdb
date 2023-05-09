package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.tinylog.Logger;

import ar.com.hjg.pngj.PngjOutputException;
import broker.Broker;
import pointcloud.Rect2d;
import rasterdb.BandProcessor;
import rasterdb.CustomWMS;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.Rasterizer;
import rasterdb.TimeBand;
import rasterdb.dsl.DSL;
import rasterdb.dsl.ErrorCollector;
import util.GeoUtil;
import util.Range2d;
import util.Web;
import util.frame.DoubleFrame;
import util.image.ImageBufferARGB;
import util.image.MonoColor;
import util.image.Renderer;

public class RasterdbMethod_webwms extends RasterdbMethod {

	public RasterdbMethod_webwms(Broker broker) {
		super(broker, "webwms");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);

		/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			Logger.error("no WMS");
			return;
		}*/		

		String reqParam = Web.getLastString(request, "Request", null);
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "REQUEST", null);
		}
		if(reqParam==null) {
			reqParam = Web.getLastString(request, "request", null);
		}
		if(reqParam==null) {
			reqParam = "GetCapabilities";
		}

		switch (reqParam) {
		case "GetMap":
			handle_GetMap(rasterdb, target, request, response, userIdentity);
			break;
		case "GetCapabilities":
			RasterdbMethod_webwms_GetCapabilities.handle_GetCapabilities(rasterdb, target, request, response, userIdentity);
			break;
		default:
			Logger.error("unknown request "+reqParam);
			return;
		}
	}

	public static void handle_GetMap(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			double[] range = null;
			double gamma = Double.NaN;
			boolean gamma_auto_sync = false;
			int[] palette = null;
			String format = Web.MIME_PNG;

			if(!target.isEmpty()) {
				Logger.info("target |" + target + "|");
				CustomWMS customWMS = rasterdb.customWmsMapReadonly.get(target);
				if(customWMS != null) {
					if(customWMS.hasValue_range()) {
						switch(customWMS.value_range) {
						case "auto":
							range = null;
							break;
						case "static":
							range = new double[] {customWMS.value_range_static_min, customWMS.value_range_static_max};
							break;
						default:
							Logger.warn("unknown value_range: " + customWMS.value_range);
						}
					}
					if(customWMS.hasGamma()) {
						if(customWMS.gamma.equals("auto")) {
							gamma = Double.NaN;
							gamma_auto_sync = customWMS.gamma_auto_sync;
						} else {
							try {
								gamma = Double.parseDouble(customWMS.gamma);
							} catch (Exception e) {
								Logger.warn("unknown gamma value: " + e + "   " + customWMS.gamma);
							}
						}
					}
					if(customWMS.hasPalette()) {
						palette = MonoColor.getPaletteDefaultNull(customWMS.palette);
					}
					if(customWMS.hasFormat()) {
						format = customWMS.format;
					}
				} else {
					Logger.warn("custom WMS not found |" + target + "|");
				}
			}


			String layers = Web.getString(request, "LAYERS", "color");
			String[] layerList = layers.split(",", -1);
			if(layerList.length > 1) {
				Logger.warn("multiple layers specified in LAYERS. Using first layer only. " + Arrays.toString(layerList));
			}
			String layer = layerList.length == 0 ? "color" : layerList[0];

			String[] lparams = layer.split("/", -1);
			String bandText = lparams.length == 0 ? "color" : lparams[0];
			int timestamp = lparams.length > 1 ? Integer.parseInt(lparams[1]) : rasterdb.rasterUnit().timeKeysReadonly().isEmpty() ? 0 : rasterdb.rasterUnit().timeKeysReadonly().last();
			if(layerList.length > 2) {
				Logger.warn("Only the two layer parameters 'bandText' or 'bandText/timestamp' are supported. " + Arrays.toString(lparams));
			}

			//String styles = Web.getString(request, "STYLES"); // STYLES parameter not used		

			int width = Web.getInt(request, "WIDTH");
			int height = Web.getInt(request, "HEIGHT");
			String[] bbox = request.getParameter("BBOX").split(",");
			//Logger.info("bbox "+Arrays.toString(bbox));
			Rect2d wmsRect = Rect2d.parseBbox(bbox);
			double[][] rePoints = wmsRect.createPoints9();
			Logger.info(Arrays.deepToString(rePoints));
			GeoReference ref = rasterdb.ref();
			if(!ref.has_code()) {
				throw new RuntimeException("no EPSG projection information");
			}
			int epsg = ref.getEPSG(0);
			if(epsg <= 0) {
				throw new RuntimeException("no EPSG projection information");
			}
			SpatialReference layerSr = GeoUtil.spatialReferenceFromEPSG(epsg);
			if(layerSr == null) {
				throw new RuntimeException("no valid EPSG projection information");
			}
			CoordinateTransformation ctToLayer = CoordinateTransformation.CreateCoordinateTransformation(GeoUtil.WEB_MERCATOR_SPATIAL_REFERENCE, layerSr);
			ctToLayer.TransformPoints(rePoints);
			Rect2d reRect = Rect2d.ofPoints(rePoints);
			Range2d range2d = ref.rect2dToRange2d(reRect);
			BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);
			ImageBufferARGB image = null;
			if(bandText.equals("color")) {
				image = Rasterizer.rasterizeRGB(processor, width, height, gamma, range, gamma_auto_sync, null);
			} else if(bandText.startsWith("band")) {
				int bandIndex = Integer.parseInt(bandText.substring(4));
				TimeBand timeBand = processor.getTimeBand(bandIndex);
				if(palette == null) {
					image = Rasterizer.rasterizeGrey(processor, timeBand, width, height, gamma, range, null);
				} else {
					image = Rasterizer.rasterizePalette(processor, timeBand, width, height, gamma, range, palette, null);	
				}
			} else {
				ErrorCollector errorCollector = new ErrorCollector();
				DoubleFrame[] doubleFrames = DSL.process(bandText, errorCollector, processor);
				if(palette == null) {
					image = Renderer.renderGreyDouble(doubleFrames[0], width, height, gamma, range);
				} else {
					image = Renderer.renderPaletteDouble(doubleFrames[0], width, height, gamma, range, palette);
				}
			}

			switch(format) {
			case "image/jpeg": // Official type from standard. set GetCapabilities.
			case "jpg": {  // needed for customWMS format selection
				response.setContentType(Web.MIME_JPEG);
				image.writeJpg(response.getOutputStream(), 0.7f);
				break;
			}
			case "jpg:small": {  // needed for customWMS format selection
				response.setContentType(Web.MIME_JPEG);
				image.writeJpg(response.getOutputStream(), 0.3f);
				break;
			}
			case "png:uncompressed": {  // needed for customWMS format selection
				response.setContentType(Web.MIME_PNG);
				image.writePng(response.getOutputStream(), 0);
				break;
			}
			case "image/png": // Official type from standard. set GetCapabilities.
			case "png":  // obsolete, needed for old code ??
			case "png:compressed": // needed for customWMS format selection
			default: {
				response.setContentType(Web.MIME_PNG);
				image.writePngCompressed(response.getOutputStream());
			}
			}
		} catch(PngjOutputException  e) {
			try {
				response.closeOutput();
			} catch(Exception e1) {
				Logger.warn(e1);
			}
			Throwable eCause = e.getCause();
			if(eCause != null && eCause instanceof EofException) {				
				Throwable eCauseSub = eCause.getCause();
				if(eCauseSub != null && eCause instanceof IOException) {
					Logger.info(eCauseSub.getMessage());
				} else {
					Logger.warn(eCause);
				}
			} else {
				Logger.warn(e);
			}			
		} catch(Exception e) {
			try {
				response.closeOutput();
			} catch(Exception e1) {
				Logger.warn(e1);
			}
			Logger.warn(e);
		}
	}	
}