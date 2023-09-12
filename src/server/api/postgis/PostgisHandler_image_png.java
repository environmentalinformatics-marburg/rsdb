package server.api.postgis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.PostgisLayer;
import util.Interruptor;
import util.Timer;
import util.Web;
import util.image.ImageBufferARGB;
import vectordb.Renderer;
import vectordb.style.BasicStyle;
import vectordb.style.Style;

public class PostgisHandler_image_png {
	
	private ConcurrentHashMap<Long, Interruptor> sessionMap = new ConcurrentHashMap<Long, Interruptor>();

	public void handle(PostgisLayer postgisLayer, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {	

		/*for (int i = 0; i < 10; i++) {
			Timer.start("Object");
			postgisLayer.forEachGeometryObject(null);
			Logger.info(Timer.stop("Object"));
		}*/

		/*for (int i = 0; i < 10; i++) {
			Timer.start("WKB");
			postgisLayer.forEachJTSGeometry(null, null);
			Logger.info(Timer.stop("WKB"));
		}*/

		Rect2d rect = postgisLayer.getExtent();
		Logger.info(rect);
		
		long session = Web.getLong(request, "session", -1);
		long sessionCnt = Web.getLong(request, "cnt", -1);
		Interruptor interruptor = null;
		if(session >= 0 && sessionCnt >= 0) {
			interruptor = new Interruptor(sessionCnt);			
			while(true) {
				Interruptor prevInterruptor = sessionMap.get(session);
				if(prevInterruptor == null) {
					if(sessionMap.putIfAbsent(session, interruptor) == null) {
						break; // new value set
					}
				} else {
					if(prevInterruptor.id < interruptor.id) {
						if(sessionMap.replace(session, prevInterruptor, interruptor)) {
							prevInterruptor.interrupted = true;
							Logger.info("set interrupted (not started) " + prevInterruptor.id + "    " + interruptor.id);
							break;  // new value set and prev interrupted
						}
					} else {
						if(prevInterruptor.id == interruptor.id) {
							Logger.warn("same id");
						}
						Logger.info("interrupted (not started)");
						return;
					}
				}
			}			
		}

		int reqWidth = Web.getInt(request, "width", -1);
		int reqHeight = Web.getInt(request, "height", -1);		
		int maxWidth = reqWidth < 1 ? (reqHeight < 1 ? 100 : reqHeight * 10) : reqWidth;			
		int maxHeight = reqHeight < 1 ? (reqWidth < 1 ? 100 : reqWidth * 10) : reqHeight;		
		double xGeoLen = rect.width();
		double yGeoLen = rect.height();
		double xTargetScale = maxWidth / xGeoLen;
		double yTargetScale = maxHeight / yGeoLen;
		double targetScale = Math.min(xTargetScale, yTargetScale);
		int width = (int) Math.ceil(targetScale * xGeoLen);
		int height = (int) Math.ceil(targetScale * yGeoLen);

		//Style style = Renderer.STYLE_DEFAULT;
		Style style = new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 0, 100), new Color(0, 150, 0, 100));
		ImageBufferARGB image = render(postgisLayer, rect, false, width, height, style, interruptor);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_PNG);
		image.writePngCompressed(response.getOutputStream());		
	}

	public static ImageBufferARGB render(PostgisLayer postgisLayer, Rect2d rect, boolean crop, int width, int height, Style style, Interruptor interruptor) {

		if(style == null) {
			style = Renderer.STYLE_DEFAULT;
		}

		ImageBufferARGB image = new ImageBufferARGB(width, height);
		Graphics2D gc = image.bufferedImage.createGraphics();
		gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gc.setColor(Color.DARK_GRAY);

		double xlen = rect.width();
		double ylen = rect.height();
		double xscale = (width - 4) / xlen;
		double yscale = (height - 4) / ylen;
		double xoff = - rect.xmin + 2 * (1 / xscale);
		double yoff = rect.ymax + 2 * (1 / yscale);
		
		String field = "class_1";		
		if(postgisLayer.hasFieldName(field)) {
			Style[] styles = new Style[] {
					new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 0, 100), new Color(0, 150, 0, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 0, 0, 100), new Color(150, 0, 0, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(0, 0, 50, 100), new Color(0, 0, 150, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 50, 0, 100), new Color(150, 150, 0, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 50, 100), new Color(0, 150, 150, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 0, 150, 100), new Color(150, 0, 150, 100)),
					
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 50, 25, 100), new Color(150, 150, 25, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(25, 50, 50, 100), new Color(25, 150, 150, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 25, 150, 100), new Color(150, 25, 150, 100)),
					
					new BasicStyle(BasicStyle.createStroke(1), new Color(25, 50, 0, 100), new Color(25, 150, 0, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 25, 0, 100), new Color(150, 25, 0, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(25, 0, 50, 100), new Color(25, 0, 150, 100)),
					
					new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 25, 100), new Color(0, 150, 25, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 0, 25, 100), new Color(150, 0, 25, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(0, 25, 50, 100), new Color(0, 25, 150, 100)),
					
					new BasicStyle(BasicStyle.createStroke(1), new Color(25, 50, 25, 100), new Color(25, 150, 25, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 25, 25, 100), new Color(150, 25, 25, 100)),
					new BasicStyle(BasicStyle.createStroke(1), new Color(25, 25, 50, 100), new Color(25, 25, 150, 100)),
					
					new BasicStyle(BasicStyle.createStroke(1), new Color(50, 50, 50, 100), new Color(150, 150, 150, 100)),					
			};
			JTSValueGeometryRasterizer jtsValueGeometryConverter = new JTSValueGeometryRasterizer(xoff, yoff, xscale, yscale, styles, gc);		
			Timer.start("render");
			postgisLayer.forEachJTSValueGeometry(rect, crop, field, interruptor, jtsValueGeometryConverter);
			Logger.info(Timer.stop("render"));
		} else {
			JTSGeometryRasterizer jtsGeometryConverter = new JTSGeometryRasterizer(xoff, yoff, xscale, yscale, style, gc);		
			Timer.start("render");
			postgisLayer.forEachJTSGeometry(rect, crop, interruptor, jtsGeometryConverter);
			Logger.info(Timer.stop("render"));
		}

		gc.dispose();

		return image;		
	}
}
