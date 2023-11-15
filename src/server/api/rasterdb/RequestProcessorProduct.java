package server.api.rasterdb;

import java.io.IOException;


import org.tinylog.Logger;

import rasterdb.BandProcessor;
import rasterdb.RasterDB;
import rasterdb.dsl.DSL;
import rasterdb.dsl.ErrorCollector;
import server.api.rasterdb.RequestProcessor.OutputProcessingType;
import util.Range2d;
import util.Receiver;
import util.frame.DoubleFrame;
import util.image.ImageBufferARGB;
import util.image.Renderer;

public class RequestProcessorProduct {
	
	
	public static void processProduct(RasterDB rasterdb, BandProcessor processor, String productText, OutputProcessingType outputProcessingType, String format, Receiver resceiver) throws IOException {
		Range2d reqRange2d = processor.getDstRange();
		int reqWidth = reqRange2d.getWidth();
		int reqHeight = reqRange2d.getHeight();
		ErrorCollector errorCollector = new ErrorCollector();
		Logger.info("process: "+productText);
		DoubleFrame[] doubleFrames = DSL.process(productText, errorCollector, rasterdb, processor);
		Logger.info("BANDS "+doubleFrames.length);
		
		switch(outputProcessingType) {
		case IDENTITY:
			switch(format) {
			case "rdat":
				RequestProcessorProductWriters.writeRdat(doubleFrames, processor, productText, resceiver);
				break;
			case "tiff":
				RequestProcessorProductWriters.writeTiff(doubleFrames, processor, productText, resceiver);
				break;			
			default:
				throw new RuntimeException("unknown format " + format);
			}
			break;
		case VISUALISATION: {
			//int per_mille = 5;
			//int per_mille = 1;
			int per_mille = 0;
			if(doubleFrames.length >= 3) {
				double gamma = Double.NaN;
				double[] range = null;
				boolean syncBands = false;
				ImageBufferARGB image = Renderer.renderRgbDouble(doubleFrames[0], doubleFrames[1], doubleFrames[2], reqWidth, reqHeight, gamma, range, syncBands, per_mille);						
				RequestProcessorProductWriters.writeImage(image, format, processor, productText, resceiver);
			} else if(doubleFrames.length >= 2) {
				double gamma = Double.NaN;
				double[] range = null;
				boolean syncBands = false;
				ImageBufferARGB image = Renderer.renderRbDouble(doubleFrames[0], doubleFrames[1], reqWidth, reqHeight, gamma, range, syncBands, per_mille);	
				RequestProcessorProductWriters.writeImage(image, format, processor, productText, resceiver);
			} else if(doubleFrames.length >= 1) {
				double gamma = Double.NaN;
				double[] range = null;
				ImageBufferARGB image = Renderer.renderGreyDouble(doubleFrames[0], reqWidth, reqHeight, gamma, range, per_mille);
				RequestProcessorProductWriters.writeImage(image, format, processor, productText, resceiver);
			} else {
				throw new RuntimeException("no bands");
			}	
			break;
		}
		default:
			throw new RuntimeException("unknown output processing type");		
		}		
	}
}
