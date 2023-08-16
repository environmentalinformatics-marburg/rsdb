package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import ar.com.hjg.pngj.PngjOutputException;
import pointcloud.Rect2d;
import rasterdb.BandProcessor;
import rasterdb.CustomWMS;
import rasterdb.FrameProducer;
import rasterdb.FrameReprojector;
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

public class RasterdbMethod_wms_GetMap {
	
	public static void handle_GetMap(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			double[] range = null;
			double gamma = Double.NaN;
			boolean gamma_auto_sync = false;
			int[] palette = null;
			String format = Web.MIME_PNG;

			CustomWMS customWMS = null;
			if(!target.isEmpty()) {
				Logger.info("target |" + target + "|");
				customWMS = rasterdb.customWmsMapReadonly.get(target);
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
					throw new RuntimeException("custom WMS not found |" + target + "|");
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
			
			boolean doReprojection = false;
			int layerEPSG = -1;
			int wmsEPSG = -1;
			String crsParameter = Web.getString(request, "CRS", null);
			if(crsParameter != null) {
				GeoReference ref = rasterdb.ref();
				if(ref.has_code()) {
					try {
						layerEPSG = ref.getEPSG(0);
						wmsEPSG = GeoUtil.parseEPSG(crsParameter, 0);
						if(layerEPSG > 0 && wmsEPSG > 0 && layerEPSG != wmsEPSG) {
							doReprojection = true;
						}
					} catch(Exception e) {
						throw new RuntimeException("CRS error");
					}
				}
			} else if(customWMS != null && customWMS.hasEPSG()) {
				wmsEPSG = customWMS.epsg;
				if(layerEPSG > 0 && wmsEPSG > 0 && layerEPSG != wmsEPSG) {
					doReprojection = true;
				}
			}

			int width = Web.getInt(request, "WIDTH");
			int height = Web.getInt(request, "HEIGHT");
			String[] bbox = request.getParameter("BBOX").split(",");
			//Logger.info("bbox "+Arrays.toString(bbox));

			FrameProducer processor;
			if(doReprojection) {
				Rect2d wmsRect = Rect2d.parseBbox(bbox);
				processor = new FrameReprojector(rasterdb, timestamp, layerEPSG, wmsEPSG, wmsRect, width, height);
			} else {
				//boolean transposed = rasterdb.ref().wms_transposed;
				boolean transposed = false;
				if(transposed) {
					Logger.info("!!!            transposed                !!!");
				}
				Range2d range2d = rasterdb.ref().parseBboxToRange2d(bbox, transposed);
				processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);
			}

			ImageBufferARGB image = null;
			if(bandText.equals("color")) {
				image = Rasterizer.rasterizeRGB(processor, rasterdb, timestamp, width, height, gamma, range, gamma_auto_sync, null);
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
				DoubleFrame[] doubleFrames = DSL.process(bandText, errorCollector, rasterdb, processor);
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
