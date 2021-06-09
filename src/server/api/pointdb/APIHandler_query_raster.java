package server.api.pointdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.Rect;
import pointdb.processing.geopoint.RasterSubGrid;
import util.Receiver;
import util.ResponseReceiver;
import util.StreamReceiver;
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

		if(format.equals("zip")) {
			String tileFormat = "tiff";
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			Receiver receiver = new StreamReceiver(zipOutputStream);
			requestRect.tiles_utmm(1000_000, 1000_000, (xtile, ytile, tileRect) -> {
				log.info(tileRect);
				String tileFilename = "tile_" + xtile + "_" + ytile + ".tiff";
				try {
					zipOutputStream.putNextEntry(new ZipEntry(tileFilename));					
					processRaster(pointdb, tileRect, processingType, tileFormat, receiver);					
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}				
			});			
			zipOutputStream.finish();
			zipOutputStream.flush();
		} else {
			processRaster(pointdb, requestRect, processingType, format, new ResponseReceiver(response));
		}
		log.info(Timer.stop("query_raster"));
	}

	private void processRaster(PointDB pointdb, Rect requestRect, String processingType, String format, Receiver receiver) throws IOException {
		RasterSubGrid[] rasterSubGrids = new RasterQueryProcessor(pointdb).process(requestRect, processingType);		

		switch(format) {
		case "rdat": {
			RdatRaster.write_RDAT_RASTER(receiver, rasterSubGrids, pointdb.config.getProj4());
			break;
		}
		case "js": {
			JsWriter.writeRaster(receiver, rasterSubGrids[0]);
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
				tiffWriter.addTiffBand(TiffBand.ofFloat64(rasterSubGrid.start_x, rasterSubGrid.start_y, rasterSubGrid.range_x, rasterSubGrid.range_y, rasterSubGrid.data, processingType));
			}
			receiver.setStatus(HttpServletResponse.SC_OK);
			receiver.setContentType("image/tiff");
			receiver.setContentLength(tiffWriter.exactSizeOfWriteAuto());
			tiffWriter.writeAuto(new DataOutputStream(receiver.getOutputStream()));
			break;
		}
		case "png": {
			RasterSubGrid r = rasterSubGrids[0];
			DoubleFrame frame = new DoubleFrame(r.copySubData(), 0, 0, 0, 0);
			ImageBufferARGB image = Renderer.renderGreyDouble(frame , frame.width, frame.height, Double.NaN, null);
			receiver.setStatus(HttpServletResponse.SC_OK);
			receiver.setContentType("image/png");
			image.writePngCompressed(receiver.getOutputStream());
			break;
		}
		default:
			throw new RuntimeException("unknown format "+format);
		}		
	}
}
