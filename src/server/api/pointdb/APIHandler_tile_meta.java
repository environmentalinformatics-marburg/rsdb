package server.api.pointdb;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.TileMeta;
import pointdb.processing.tilemeta.TileMetaConsumer;
import util.CsvWriter;


public class APIHandler_tile_meta extends PointdbAPIHandler {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger();	
	
	public APIHandler_tile_meta(Broker broker) {
		super(broker, "tile_meta.csv");		
	}
	
	interface F<T> {
		int get(T e);
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);
		
		PointDB pointdb = getPointdb(request);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/plain;charset=utf-8");
		PrintWriter out = response.getWriter();
		
		CsvWriter<TileMeta> csvwriter = new CsvWriter<TileMeta>(out);
		csvwriter.addInt("x", TileMeta::getX);
		csvwriter.addInt("y", TileMeta::getY);
		csvwriter.addInt("point_count", TileMeta::getPoint_count);
		csvwriter.addInt("intensity_min", t->t.min.intensity);
		csvwriter.addInt("intensity_max", t->t.max.intensity);
		csvwriter.addInt("intensity_avg", t->t.avg.intensity);
		csvwriter.addInt("z_min", t->t.min.z);
		csvwriter.addInt("z_max", t->t.max.z);
		csvwriter.addInt("z_avg", t->t.avg.z);
		

		csvwriter.writeHeaderRow();
		
		TileMetaConsumer consumer = new TileMetaConsumer() {			
			@Override
			public void nextTileMeta(TileMeta tileMeta) {
				csvwriter.writeRow(tileMeta);				
			}
		};
		pointdb.tileMetaProducer(null).produce(consumer);	
	}

}
