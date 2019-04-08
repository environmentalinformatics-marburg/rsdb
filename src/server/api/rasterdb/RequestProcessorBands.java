package server.api.rasterdb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jetty.server.Response;

import rasterdb.Rasterizer;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import server.api.rasterdb.RequestProcessor.OutputProcessingType;
import server.api.rasterdb.WmsHandler.Interruptor;
import util.Range2d;
import util.image.ImageBufferARGB;

public class RequestProcessorBands {
	//private static final Logger log = LogManager.getLogger();

	public static abstract class Receiver {
		public abstract OutputStream getOutputStream() throws IOException;
		public abstract void setStatus(int sc);
		public abstract void setContentType(String contentType);
	}

	public static class ResponseReceiver extends Receiver {
		private final Response response;
		
		public ResponseReceiver(Response response) {
			this.response = response;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return response.getOutputStream();
		}

		@Override
		public void setStatus(int sc) {
			response.setStatus(sc);
			
		}

		@Override
		public void setContentType(String contentType) {
			response.setContentType(contentType);			
		}
	}
	
	public static class StreamReceiver extends Receiver {
		
		private final OutputStream out;

		public StreamReceiver(OutputStream out) {
			this.out = out;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return out;
		}

		@Override
		public void setStatus(int sc) {
		}

		@Override
		public void setContentType(String contentType) {
		}
		
	}

	public static void processBands(TimeBandProcessor processor, Collection<TimeBand> processingBands, OutputProcessingType outputProcessingType, String format, Receiver resceiver) throws IOException {
		Range2d reqRange2d = processor.getDstRange();
		int reqWidth = reqRange2d.getWidth();
		int reqHeight = reqRange2d.getHeight();

		switch(outputProcessingType) {
		case IDENTITY:
			switch(format) {
			case "rdat":
				RequestProcessorBandsWriters.writeRdat(processor, processingBands, resceiver);					
				break;
			case "tiff":
				RequestProcessorBandsWriters.writeTiff(processor, processingBands, resceiver);
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
				RequestProcessorProductWriters.writeImage(image, format, processor, "visualisation", resceiver);	
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
				RequestProcessorProductWriters.writeImage(image, format, processor, "visualisation", resceiver);						
			} else if(processingBands.size() >= 1) {
				TimeBand band = processingBands.iterator().next();
				double gamma = Double.NaN;
				double[] range = null;
				Interruptor interruptor = null;
				ImageBufferARGB image = Rasterizer.rasterizeGrey(processor, band, reqWidth, reqHeight, gamma, range, interruptor);
				RequestProcessorProductWriters.writeImage(image, format, processor, "visualisation", resceiver);	
			} else {
				throw new RuntimeException("no bands");
			}					
			break;
		default:
			throw new RuntimeException("unknown output processing type");

		}


	}

}
