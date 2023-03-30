package server.api.pointdb;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import pointdb.processing.tilepoint.PointFilter;
import pointdb.processing.tilepoint.ImageCreator;
import pointdb.processing.tilepoint.ImageType;
import util.Timer;
import util.Web;

public class APIHandler_image extends PointdbAPIHandler {
	

	private static final int TILE_LOCAL_TO_SCREEN_DIV_DEFAULT = 500;

	public APIHandler_image(Broker broker) {
		super(broker, "image");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Timer.start("image");
		//Logger.info("get image");
		request.setHandled(true);
		PointDB pointdb = getPointdb(request);
		double x = Web.getDouble(request, "x");
		double y = Web.getDouble(request, "y");
		int utm_center_x = PdbConst.to_utm(x);
		int utm_center_y = PdbConst.to_utm(y);
		int width = (int) Web.getDouble(request, "width");
		int height = (int) Web.getDouble(request, "height");
		int TILE_LOCAL_TO_SCREEN_DIV = Web.getInt(request, "scale", TILE_LOCAL_TO_SCREEN_DIV_DEFAULT);
		boolean fill = Web.getBoolean(request, "fill", false);
		ImageType type = ImageType.parse(request.getParameter("type"), ImageType.INTENSITY_Z);

		PointFilter filter = null;
		String filterText = request.getParameter("filter");
		if(filterText!=null) {
			filter = PointFilter.createFilter(filterText);
			Logger.info("filter: "+filterText+" "+filter);
		}

		Rect rect = Rect.of_utm_center(utm_center_x, utm_center_y, width, height, TILE_LOCAL_TO_SCREEN_DIV);
		Statistics statistics = pointdb.tileMetaProducer(rect).toStatistics();
		ImageCreator imageCreator = ImageCreator.of(pointdb.tilePointProducer(rect).filter(filter), statistics, rect, width, height, TILE_LOCAL_TO_SCREEN_DIV, fill, type);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_PNG);
		imageCreator.create().writePngUncompressed(response.getOutputStream());
		Logger.info(Timer.stop("image"));
	}
}
