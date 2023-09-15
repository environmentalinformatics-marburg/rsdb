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
import postgis.style.RotatingArrayStyleProvider;
import postgis.style.StyleJtsGeometryRasterizer;
import postgis.style.StyleProvider;
import util.Interruptor;
import util.Timer;
import util.Web;
import util.image.ImageBufferARGB;
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

		
		ImageBufferARGB image = render(postgisLayer, rect, false, width, height, postgisLayer.getStyleProvider(), interruptor);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_PNG);
		image.writePngCompressed(response.getOutputStream());		
	}

	public static ImageBufferARGB render(PostgisLayer postgisLayer, Rect2d rect, boolean crop, int width, int height, StyleProvider styleProvider, Interruptor interruptor) {
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
		
		StyleJtsGeometryRasterizer styleJtsGeometryRasterizer = new StyleJtsGeometryRasterizer(xoff, yoff, xscale, yscale, gc);
		
		String field = styleProvider.getValueField();
		if(field == null) {
			field = "class_1";
		}
		
		if(field != null && postgisLayer.hasFieldName(field)) {
			Timer.start("render");
			postgisLayer.forEachIntJtsGeometry(rect, crop, field, interruptor, (value, geometry) -> {
				Style style = styleProvider.getStyleByValue(value);
				styleJtsGeometryRasterizer.acceptGeometry(style, geometry);
			});
			Logger.info(Timer.stop("render"));
		} else {
			final Style style = styleProvider.getStyle();
			Timer.start("render");
			postgisLayer.forEachJtsGeometry(rect, crop, interruptor, geometry -> styleJtsGeometryRasterizer.acceptGeometry(style, geometry));
			Logger.info(Timer.stop("render"));
		}

		gc.dispose();

		return image;		
	}
}
