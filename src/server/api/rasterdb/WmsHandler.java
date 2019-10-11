package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public WmsHandler(Broker broker) {
		this.broker = broker;
	}

	// http://localhost:8081/rasterdb_wms?Request=GetCapabilities

	@Override
	public void handle(String target, Request request, HttpServletRequest internal, HttpServletResponse response)
			throws IOException, ServletException {
		//log.info(request);
		request.setHandled(true);

		try {

			/*if(!"WMS".equals(request.getParameter("SERVICE"))) {
			log.error("no WMS");
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
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}
	}

	private void handle_GetCapabilities(String target, Request request, HttpServletRequest internal, HttpServletResponse response) throws IOException {
		WmsCapabilities wmsCapabilities = new WmsCapabilities(broker, request.getRequestURL().toString());
		response.setContentType("application/xml");
		wmsCapabilities.capabilities(response.getOutputStream(), Web.getUserIdentity(request));		
	}

	public static class InterruptorInterruptedException extends RuntimeException {

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
			//log.info("checkInterrupted " + interruptor);
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
						//log.info("session " + session);
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
										log.warn("same id");
									}
									log.info("****************************************** interrupted (not started) ***************************************");
									return;
								}
							}
						}
					} catch(Exception e) {
						log.warn(e);
					}
				}
			}

			String format = request.getParameter("FORMAT");
			if(format == null) {
				format = "image/png";
			}

			String layer = request.getParameter("LAYERS");
			log.info("layer "+layer);
			RasterDB rasterdb = broker.getRasterdb(layer);
			if (!rasterdb.isAllowed(Web.getUserIdentity(request))) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("access not allowed for user");
				log.error("access not allowed for user");
				return;
			}
			int timestamp = 0;
			String timeText = request.getParameter("TIME");
			if(timeText == null) {
				try {
					timestamp = rasterdb.rasterUnit().timeKeysReadonly().last();
				} catch(Exception e) {
					if(rasterdb.rasterUnit().timeKeysReadonly().isEmpty()) {
						log.warn("empty rasterdb layer");
						return;
					} else {
						throw e;
					}
				}
			} else {
				timestamp = Integer.parseInt(timeText);
			}
			String[] bbox = request.getParameter("BBOX").split(",");
			log.info("bbox "+Arrays.toString(bbox));
			GeoReference ref = rasterdb.ref();
			String modus = request.getParameter("modus");
			boolean transposed = modus != null && modus.equals("openlayers") ? ref.wms_transposed : false;
			Range2d range2d = ref.parseBboxToRange2d(bbox, transposed);
			//Range2d range2d = ref.parseBboxToRange2d(bbox, ref.wms_transposed);
			//log.info(range2d);
			//log.info("geo xmin "+ref.pixelXToGeo(range2d.xmin)+"  ymin "+ref.pixelYToGeo(range2d.ymin)+"  xmax "+ref.pixelXToGeo(range2d.xmax)+"  ymax "+ref.pixelYToGeo(range2d.ymax));
			String widthText = request.getParameter("WIDTH");
			int width = Integer.parseInt(widthText);
			String heightText = request.getParameter("HEIGHT");
			int height = Integer.parseInt(heightText);

			BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, width, height);

			String style = request.getParameter("STYLES");	

			PureImage image = null;
			String[] styles = style.split(" ");
			String styles_bands = styles[0];
			String minText = null;
			String maxText = null;
			String gammaText = null;
			String palText = "grey";
			boolean syncBands = false;
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
					log.warn("unknown style: " + text);
				}
			}
			double[] range = null;
			if(minText != null && maxText != null) {
				try {
					double min = Double.parseDouble(minText);
					double max = Double.parseDouble(maxText);
					log.info("minmax "+min+"  "+max);
					range = new double[]{min, max};
				} catch (Exception e) {
					log.warn(e);
				}
			}
			double gamma = Double.NaN;
			if(gammaText != null) {
				try {
					gamma = Double.parseDouble(gammaText);
				} catch (Exception e) {
					log.warn(e);
				}
			}
			int[] palette = MonoColor.getPaletteDefaultNull(palText);
			if(Interruptor.isInterrupted(currentInterruptor)) {
				log.info("****************************************** interrupted (pre load)*******************************************");
				return;
			}
			if(styles_bands.equals("color") || styles_bands.isEmpty()) {
				Timer.start("render");
				image = Rasterizer.rasterizeRGB(processor, width, height, gamma, range, syncBands, currentInterruptor);
				log.info(Timer.stop("render"));
			} else if(styles_bands.startsWith("band")) {
				String s = styles_bands.substring(4);
				int bandIndex = Integer.parseInt(s);
				TimeBand band = processor.getTimeBand(bandIndex);
				Timer.start("render");
				if(palette == null) {
					image = Rasterizer.rasterizeGrey(processor, band, width, height, gamma, range, currentInterruptor);
				} else {
					image = Rasterizer.rasterizePalette(processor, band, width, height, gamma, range, palette, currentInterruptor);		
				}
				log.info(Timer.stop("render"));
			} else {
				ErrorCollector errorCollector = new ErrorCollector();
				DoubleFrame[] doubleFrames = DSL.process(styles_bands, errorCollector, processor);
				if(doubleFrames.length < 1) {
					throw new RuntimeException("no result");
				}
				Timer.start("render");
				Interruptor.checkInterrupted(currentInterruptor);
				if(palette == null) {
					image = Renderer.renderGreyDouble(doubleFrames[0], width, height, gamma, range);
				} else {
					image = Renderer.renderPaletteDouble(doubleFrames[0], width, height, gamma, range, palette);					
				}
				log.info(Timer.stop("render"));
			}
			if(Interruptor.isInterrupted(currentInterruptor)) {
				log.info("****************************************** interrupted (pre sent)*******************************************");
				return;
			}
			Timer.start("compress/transfer");
			log.info("");
			response.setStatus(HttpServletResponse.SC_OK);
			switch(format) {
			case "image/jpeg": {
				response.setContentType("image/jpeg");
				image.writeJpg(response.getOutputStream(), 0.7f);
				break;
			}
			case "image/png": {
				response.setContentType("image/png");
				image.writePngCompressed(response.getOutputStream());
				break;
			}
			default: {
				if(format.startsWith("image/png:")) {
					response.setContentType("image/png");
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
					response.setContentType("image/png");
					image.writePngCompressed(response.getOutputStream());
				}
			}
			}
			log.info(Timer.stop("compress/transfer"));
			log.info(Timer.stop("full request"));
			if(Interruptor.isInterrupted(currentInterruptor)) {
				log.info("****************************************** interrupted *******************************************");
			}
			//ImageRGBA.ofBufferedImage(bi).writePngUncompressed(response.getOutputStream());

		} catch(InterruptorInterruptedException e) {
			log.info("****************************************** interrupted (checked)*******************************************");
			return;
		} finally {
			if(session != null && currentInterruptor != null) {
				if(taskMap.remove(session, currentInterruptor)) {
					//log.info("task removed");
				}
			}
			//log.info("session map size " + taskMap.size() + " +++++++++++++++++++++++++++++++++++++");
		}
	}
}
