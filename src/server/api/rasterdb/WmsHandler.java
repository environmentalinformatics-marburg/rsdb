package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.Broker;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.Rasterizer;
import rasterdb.TimeBand;
import rasterdb.dsl.DSL;
import rasterdb.dsl.ErrorCollector;
import server.api.main.APIHandler_session;
import util.Range2d;
import util.Timer;
import util.Web;
import util.frame.DoubleFrame;
import util.image.MonoColor;
import util.image.PureImage;
import util.image.Renderer;

public class WmsHandler extends AbstractHandler {
	

	private final Broker broker;

	public WmsHandler(Broker broker) {
		this.broker = broker;
	}

	// http://localhost:8081/rasterdb_wms?Request=GetCapabilities

	@Override
	public void handle(String target, Request request, HttpServletRequest internal, HttpServletResponse response)
			throws IOException, ServletException {
		//Logger.info(request);
		request.setHandled(true);

		try {

			/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			Logger.error("no WMS");
			return;
		}*/

			String reqParam = request.getParameter("Request");
			if(reqParam==null) {
				reqParam = request.getParameter("REQUEST");
			}
			if(reqParam==null) {
				reqParam = request.getParameter("request");
			}


			if(reqParam==null) {
				throw new RuntimeException("no REQUEST parameter");
			}

			switch (reqParam) {
			case "GetMap":
				handle_GetMap(target, request, internal, response);
				break;
			case "GetCapabilities":
				handle_GetCapabilities(target, request, internal, response);
				break;
			default:
				throw new RuntimeException("unknown request "+reqParam);
			}

		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}
	}

	private void handle_GetCapabilities(String target, Request request, HttpServletRequest internal, HttpServletResponse response) throws IOException {
		WmsCapabilities wmsCapabilities = new WmsCapabilities(broker, request.getRequestURL().toString());
		response.setContentType(Web.MIME_XML);
		wmsCapabilities.capabilities(response.getOutputStream(), Web.getUserIdentity(request));		
	}

	public static class InterruptorInterruptedException extends RuntimeException {
		private static final long serialVersionUID = -8405416244955848549L;
	}

	public static class Interruptor {

		public volatile boolean interrupted;
		public final long id;

		public Interruptor(long id) {
			this.id = id;
		}

		public static boolean isInterrupted(Interruptor interruptor) {
			return interruptor != null && interruptor.interrupted;
		}

		/**
		 * 
		 * @param interruptor nullable
		 */
		public static void checkInterrupted(Interruptor interruptor) {
			//Logger.info("checkInterrupted " + interruptor);
			if(interruptor != null && interruptor.interrupted) {
				throw new InterruptorInterruptedException();
			}
		}
	}

	private static ConcurrentHashMap<Long, Interruptor> taskMap = new ConcurrentHashMap<Long, Interruptor>();

	private void handle_GetMap(String target, Request request, HttpServletRequest internal, HttpServletResponse response) throws IOException {
		Interruptor currentInterruptor = null;
		Long session = null;
		try {
			Timer.start("full request");
			String base64Session = request.getParameter("session");
			if(base64Session != null) {
				String wmsReqestCountText = request.getParameter("cnt");
				if(wmsReqestCountText != null) {
					try {
						session = APIHandler_session.decodeSession(base64Session);
						//Logger.info("session " + session);
						long wmsReqestCount = Long.parseLong(wmsReqestCountText);
						currentInterruptor = new Interruptor(wmsReqestCount);
						while(true) {
							Interruptor prevInterruptor = taskMap.get(session);
							if(prevInterruptor == null) {
								if(taskMap.putIfAbsent(session, currentInterruptor) == null) {
									break; // new value set
								}
							} else {
								if(prevInterruptor.id < currentInterruptor.id) {
									if(taskMap.replace(session, prevInterruptor, currentInterruptor)) {
										prevInterruptor.interrupted = true;
										break;  // new value set and prev interrupted
									}
								} else {
									if(prevInterruptor.id == currentInterruptor.id) {
										Logger.warn("same id");
									}
									Logger.info("****************************************** interrupted (not started) ***************************************");
									return;
								}
							}
						}
					} catch(Exception e) {
						Logger.warn(e);
					}
				}
			}

			String format = request.getParameter("FORMAT");
			if(format == null) {
				format = Web.MIME_PNG;
			}

			String layer = request.getParameter("LAYERS");
			//Logger.info("layer "+layer);
			RasterDB rasterdb = broker.getRasterdb(layer);
			if (!rasterdb.isAllowed(Web.getUserIdentity(request))) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType(Web.MIME_TEXT);
				response.getWriter().println("access not allowed for user");
				Logger.error("access not allowed for user");
				return;
			}
			int timestamp = 0;
			String timeText = request.getParameter("TIME");
			if(timeText == null) {
				try {
					timestamp = rasterdb.rasterUnit().timeKeysReadonly().last();
				} catch(Exception e) {
					if(rasterdb.rasterUnit().timeKeysReadonly().isEmpty()) {
						Logger.warn("empty rasterdb layer");
						return;
					} else {
						throw e;
					}
				}
			} else {
				timestamp = Integer.parseInt(timeText);
			}
			String[] bbox = request.getParameter("BBOX").split(",");
			Logger.info("bbox "+Arrays.toString(bbox));
			GeoReference ref = rasterdb.ref();
			String modus = request.getParameter("modus");
			boolean transposed = modus != null && modus.equals("openlayers") ? ref.wms_transposed : false;
			Range2d range2d = ref.parseBboxToRange2d(bbox, transposed);
			//Range2d range2d = ref.parseBboxToRange2d(bbox, ref.wms_transposed);
			//Logger.info(range2d);
			//Logger.info("geo xmin "+ref.pixelXToGeo(range2d.xmin)+"  ymin "+ref.pixelYToGeo(range2d.ymin)+"  xmax "+ref.pixelXToGeo(range2d.xmax)+"  ymax "+ref.pixelYToGeo(range2d.ymax));
			String widthText = request.getParameter("WIDTH");
			int width = Integer.parseInt(widthText);
			String heightText = request.getParameter("HEIGHT");
			int height = Integer.parseInt(heightText);

			BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);

			String style_product = "color";
			double gamma = Double.NaN;
			double[] range = null;
			boolean syncBands = false;
			int[] palette = null;

			{// style processing
				String minText = null;
				String maxText = null;
				String gammaText = null;
				String palText = "grey";
				String style = request.getParameter("STYLES");
				//Logger.info("STYLES: |" + style + "|");
				if(style != null) {
					String[] styles = style.trim().split("@");
					//Logger.info("styles " + Arrays.toString(styles));
					if(styles.length > 0) {
						style_product = styles[0];
						for (int i = 1; i < styles.length; i++) {
							String text = styles[i];
							if(text.startsWith("min")) {
								minText = text.substring(3);
							} else if(text.startsWith("max")) {
								maxText = text.substring(3);
							} else if(text.startsWith("gamma")) {
								gammaText = text.substring(5);
							} else if(text.equals("sync_bands")) {
								syncBands = true;
							} else if(text.startsWith("pal_")) {
								palText = text.substring(4);
							} else {
								Logger.warn("unknown style: " + text);
							}
						}
					}
				}
				if(minText != null && maxText != null) {
					try {
						double min = Double.parseDouble(minText);
						double max = Double.parseDouble(maxText);
						//Logger.info("minmax "+min+"  "+max);
						range = new double[]{min, max};
					} catch (Exception e) {
						Logger.warn(e);
					}
				}
				if(gammaText != null) {
					try {
						gamma = Double.parseDouble(gammaText);
					} catch (Exception e) {
						Logger.warn(e);
					}
				}
				palette = MonoColor.getPaletteDefaultNull(palText);
			} // style processing end

			if(Interruptor.isInterrupted(currentInterruptor)) {
				Logger.info("****************************************** interrupted (pre load)*******************************************");
				return;
			}
			PureImage image = null;
			if(style_product.equals("color") || style_product.isEmpty()) {
				Timer.start("render");
				image = Rasterizer.rasterizeRGB(processor, rasterdb, timestamp, width, height, gamma, range, syncBands, currentInterruptor);
				//Logger.info(Timer.stop("render"));
			} else if(style_product.startsWith("band")) {
				String s = style_product.substring(4);
				int bandIndex = Integer.parseInt(s);
				TimeBand band = processor.getTimeBand(bandIndex);
				Timer.start("render");
				if(palette == null) {
					image = Rasterizer.rasterizeGrey(processor, band, width, height, gamma, range, currentInterruptor);
				} else {
					image = Rasterizer.rasterizePalette(processor, band, width, height, gamma, range, palette, currentInterruptor);		
				}
				//Logger.info(Timer.stop("render"));
			} else {
				ErrorCollector errorCollector = new ErrorCollector();
				DoubleFrame[] doubleFrames = DSL.process(style_product, errorCollector, rasterdb, processor);
				if(doubleFrames.length < 1) {
					throw new RuntimeException("no result");
				}
				Timer.start("render");
				Interruptor.checkInterrupted(currentInterruptor);
				//Logger.info("frames " + doubleFrames.length);
				if(doubleFrames.length < 1) {
					// nothing
				} else if(doubleFrames.length == 1) {
					if(palette == null) {
						image = Renderer.renderGreyDouble(doubleFrames[0], width, height, gamma, range);
					} else {
						image = Renderer.renderPaletteDouble(doubleFrames[0], width, height, gamma, range, palette);					
					}
				} else if(doubleFrames.length == 2) {
					image = Renderer.renderRbDouble(doubleFrames[0], doubleFrames[1], width, height, gamma, range, syncBands);
				} else {
					image = Renderer.renderRgbDouble(doubleFrames[0], doubleFrames[1], doubleFrames[2], width, height, gamma, range, syncBands);
				}				
				//Logger.info(Timer.stop("render"));
			}
			if(Interruptor.isInterrupted(currentInterruptor)) {
				Logger.info("****************************************** interrupted (pre sent)*******************************************");
				return;
			}
			Timer.start("compress/transfer");
			//Logger.info("");
			response.setStatus(HttpServletResponse.SC_OK);
			switch(format) {
			case "image/jpeg": {
				response.setContentType(Web.MIME_JPEG);
				image.writeJpg(response.getOutputStream(), 0.7f);
				break;
			}
			case "image/png": {
				response.setContentType(Web.MIME_PNG);
				image.writePngCompressed(response.getOutputStream());
				break;
			}
			default: {
				if(format.startsWith("image/png:")) {
					response.setContentType(Web.MIME_PNG);
					switch(format) {
					case "image/png:0":
						image.writePng(response.getOutputStream(), 0);
						break;
					case "image/png:1":
						image.writePng(response.getOutputStream(), 1);
						break;
					case "image/png:2": 
						image.writePng(response.getOutputStream(), 2);
						break;
					case "image/png:3": 
						image.writePng(response.getOutputStream(), 3);
						break;	
					case "image/png:4": 
						image.writePng(response.getOutputStream(), 4);
						break;
					case "image/png:5": 
						image.writePng(response.getOutputStream(), 5);
						break;
					case "image/png:6": 
						image.writePng(response.getOutputStream(), 6);
						break;
					case "image/png:7": 
						image.writePng(response.getOutputStream(), 7);
						break;
					case "image/png:8": 
						image.writePng(response.getOutputStream(), 8);
						break;
					case "image/png:9": 
						image.writePng(response.getOutputStream(), 9);
						break;
					default: 
						image.writePngCompressed(response.getOutputStream());
					}
				} else {
					response.setContentType(Web.MIME_PNG);
					image.writePngCompressed(response.getOutputStream());
				}
			}
			}
			//Logger.info(Timer.stop("compress/transfer"));
			Logger.info(Timer.stop("full request"));
			if(Interruptor.isInterrupted(currentInterruptor)) {
				Logger.info("****************************************** interrupted *******************************************");
			}
			//ImageRGBA.ofBufferedImage(bi).writePngUncompressed(response.getOutputStream());

		} catch(InterruptorInterruptedException e) {
			Logger.info("****************************************** interrupted (checked)*******************************************");
			return;
		} finally {
			if(session != null && currentInterruptor != null) {
				if(taskMap.remove(session, currentInterruptor)) {
					//Logger.info("task removed");
				}
			}
			//Logger.info("session map size " + taskMap.size() + " +++++++++++++++++++++++++++++++++++++");
		}
	}
}
