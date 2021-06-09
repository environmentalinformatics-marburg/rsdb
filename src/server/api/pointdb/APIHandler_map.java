package server.api.pointdb;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.processing.tilemeta.MetaImageCreator;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import pointdb.processing.tilemeta.TileMetaAggregator.Aggregator;
import util.Timer;
import util.Web;
import util.image.ImageRGBA;

public class APIHandler_map extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();
	
	public APIHandler_map(Broker broker) {
		super(broker, "map");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		//log.info("get map");
		request.setHandled(true);
		
		PointDB pointdb = getPointdb(request);
		int tile_size = (int) Web.getDouble(request, "tile_size");
		int x = (int) Web.getDouble(request, "x");
		int y = (int) Web.getDouble(request, "y");
		int width = (int) Web.getDouble(request, "width");
		int height = (int) Web.getDouble(request, "height");
		double gamma = Web.getDouble(request, "gamma", 1);

		ImageRGBA imageRGBA = null;
		//for (int i = 0; i < 10; i++) {		
		Timer.start("meta");		
		Aggregator aggregate = pointdb.tileMetaProducer(null).aggregate(tile_size).produceAggregator();		
		Statistics stat = aggregate.toStatistics();		
		imageRGBA = new MetaImageCreator(aggregate, stat, tile_size, x, y, width, height).create(gamma);
		log.info(Timer.stop("meta"));
		//}
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("image/png");
		imageRGBA.writePngUncompressed(response.getOutputStream());

	}
}
