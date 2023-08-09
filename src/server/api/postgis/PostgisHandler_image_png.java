package server.api.postgis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.PostgisLayer;
import util.Timer;
import util.Web;
import util.image.ImageBufferARGB;
import vectordb.Renderer;
import vectordb.style.BasicStyle;
import vectordb.style.Style;

public class PostgisHandler_image_png {

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
		ImageBufferARGB image = render(postgisLayer, rect, width, height, style);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_PNG);
		image.writePngCompressed(response.getOutputStream());		
	}

	private ImageBufferARGB render(PostgisLayer postgisLayer, Rect2d rect, int width, int height, Style style) {

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

		//GeometryConverter geometryConverter = new GeometryConverter(xoff, yoff, xscale, yscale, style, gc);
		JTSGeometryConverter jtsGeometryConverter = new JTSGeometryConverter(xoff, yoff, xscale, yscale, style, gc);

		/*for (int r = 0; r < 20; r++) {	
			Timer.start("forEachGeometry");
			for (int i = 0; i < 10; i++) {
				
				postgisLayer.forEachGeometry(null, geometryConverter);
				
			}
			Logger.info(Timer.stop("forEachGeometry"));
			Timer.start("forEachJTSGeometry");
			for (int i = 0; i < 10; i++) {
				
				postgisLayer.forEachJTSGeometry(null, jtsGeometryConverter);
				
			}
			Logger.info(Timer.stop("forEachJTSGeometry"));
		}*/
		//postgisLayer.forEachGeometry(null, geometryConverter);
		postgisLayer.forEachJTSGeometry(null, jtsGeometryConverter);

		gc.dispose();

		return image;		
	}
}
