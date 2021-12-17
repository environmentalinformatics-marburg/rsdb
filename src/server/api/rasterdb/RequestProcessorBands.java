package server.api.rasterdb;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import rasterdb.Rasterizer;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import server.api.rasterdb.RequestProcessor.OutputProcessingType;
import server.api.rasterdb.WmsHandler.Interruptor;
import util.Range2d;
import util.Receiver;
import util.image.ImageBufferARGB;

public class RequestProcessorBands {
	//

	public static void processBands(TimeBandProcessor processor, Collection<TimeBand> processingBands, OutputProcessingType outputProcessingType, String format, Receiver receiver) throws IOException {
		Range2d reqRange2d = processor.getDstRange();
		int reqWidth = reqRange2d.getWidth();
		int reqHeight = reqRange2d.getHeight();

		switch(outputProcessingType) {
		case IDENTITY:
			switch(format) {
			case "rdat":
				RequestProcessorBandsWriters.writeRdat(processor, processingBands, receiver);					
				break;
			case "tiff":
			case "tiff:banded":
				RequestProcessorBandsWriters.writeTiff(processor, processingBands, receiver);
				break;
			case "tiff:banded:tiled":
				RequestProcessorBandsWriters.writeTiffTiled(processor, processingBands, receiver);
				break;					
			default:
				throw new RuntimeException("unknown format " + format);
			}
			break;
		case VISUALISATION:
			if(processingBands.size() >= 3) {
				Iterator<TimeBand> it = processingBands.iterator();
				TimeBand[] timebands = {it.next(), it.next(), it.next()};
				double gamma = Double.NaN;
				double[] range = null;
				boolean syncBands = false;
				Interruptor interruptor = null;
				ImageBufferARGB image = Rasterizer.rasterizeRGB(processor, timebands, reqWidth, reqHeight, gamma, range, syncBands, interruptor);
				RequestProcessorProductWriters.writeImage(image, format, processor, "visualisation", receiver);	
			} else if(processingBands.size() >= 2) {
				Iterator<TimeBand> it = processingBands.iterator();
				TimeBand r = it.next();
				TimeBand b = it.next();
				TimeBand[] bands = {r, r, b};
				double gamma = Double.NaN;
				double[] range = null;
				boolean syncBands = false;
				Interruptor interruptor = null;
				ImageBufferARGB image = Rasterizer.rasterizeRGB(processor, bands, reqWidth, reqHeight, gamma, range, syncBands, interruptor);
				RequestProcessorProductWriters.writeImage(image, format, processor, "visualisation", receiver);						
			} else if(processingBands.size() >= 1) {
				TimeBand band = processingBands.iterator().next();
				double gamma = Double.NaN;
				double[] range = null;
				Interruptor interruptor = null;
				ImageBufferARGB image = Rasterizer.rasterizeGrey(processor, band, reqWidth, reqHeight, gamma, range, interruptor);
				RequestProcessorProductWriters.writeImage(image, format, processor, "visualisation", receiver);	
			} else {
				throw new RuntimeException("no bands");
			}					
			break;
		default:
			throw new RuntimeException("unknown output processing type");

		}


	}

}
