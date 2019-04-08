package server.api.pointdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.Rect;
import pointdb.processing.geopoint.RasterSubGrid;
import util.Timer;
import util.Web;
import util.frame.DoubleFrame;
import util.image.ImageBufferARGB;
import util.image.Renderer;
import util.rdat.RdatRaster;
import util.tiff.TiffBand;
import util.tiff.TiffWriter;

public class APIHandler_query_raster extends PointdbAPIHandler {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger();

	public APIHandler_query_raster(Broker broker) {
		super(broker, "query_raster");
	}		

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Timer.start("query_raster");
		PointDB pointdb = getPointdb(request);		
		Rect requestRect = Rect.of_extent_request(request).outerMeterRect();


		String format = Web.getString(request, "format", "rdat");

		String[] processingTypes = Web.getStrings(request, "type");
		if(processingTypes.length!=1) {
			throw new RuntimeException("currently only one type per query supported: "+Arrays.toString(processingTypes));
		}		
		String processingType = processingTypes[0].toLowerCase();


		RasterSubGrid[] rasterSubGrids = new RasterQueryProcessor(pointdb).process(requestRect, processingType);		

		switch(format) {
		case "rdat": {
			RdatRaster.write_RDAT_RASTER(response, rasterSubGrids, pointdb.config.getProj4());
			break;
		}
		case "js": {
			JsWriter.writeRaster(response, rasterSubGrids[0]);
			break;
		}
		case "tiff": {
			int width = rasterSubGrids[0].range_x;
			int height = rasterSubGrids[0].range_y;
			double geoXmin = rasterSubGrids[0].local_min_x;
			double geoYmin = rasterSubGrids[0].local_min_y;
			double xScale = 1;
			double yScale = 1;
			TiffWriter tiffWriter = new TiffWriter(width, height, geoXmin, geoYmin, xScale, yScale, (short)pointdb.config.getEPSG());
			for(RasterSubGrid rasterSubGrid:rasterSubGrids) {
				log.info(rasterSubGrid);
				tiffWriter.addTiffBand(TiffBand.ofFloat64(rasterSubGrid.start_x, rasterSubGrid.start_y, rasterSubGrid.range_x, rasterSubGrid.range_y, rasterSubGrid.data));
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("image/tiff");
			//tiffWriter.writeTIFF(new DataOutputStream(response.getOutputStream()));
			tiffWriter.writeAuto(new DataOutputStream(response.getOutputStream()));
			break;
		}
		case "png": {
			RasterSubGrid r = rasterSubGrids[0];
			DoubleFrame frame = new DoubleFrame(r.copySubData(), 0, 0, 0, 0);
			ImageBufferARGB image = Renderer.renderGreyDouble(frame , frame.width, frame.height, Double.NaN, null);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("image/png");
			image.writePngCompressed(response.getOutputStream());
			break;
		}
		default:
			throw new RuntimeException("unknown format "+format);
		}
		log.info(Timer.stop("query_raster"));
	}
}
