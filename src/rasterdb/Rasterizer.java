package rasterdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.api.rasterdb.WmsHandler.Interruptor;
import util.Timer;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;
import util.image.ImageBufferARGB;
import util.image.Renderer;

public class Rasterizer {
	static final Logger log = LogManager.getLogger();
	
	public static ImageBufferARGB rasterizeRGB(BandProcessor processor, int width, int height, double gamma, double[] range, boolean syncBands, Interruptor interruptor) {
		TimeBand[] bands = TimeBand.of(processor.timestamp, BandProcessing.getBestColorBands(processor.rasterdb));
		return rasterizeRGB(processor, bands, width, height, gamma, range, syncBands, interruptor);
	}

	public static ImageBufferARGB rasterizeRGB(TimeBandProcessor processor, TimeBand[] bands, int width, int height, double gamma, double[] range, boolean syncBands, Interruptor interruptor) {
		Timer.start("load");
		if(bands[0].band.isTypeShortOrExactConvertible() && bands[1].band.isTypeShortOrExactConvertible() && bands[2].band.isTypeShortOrExactConvertible()) {
			if((bands[0].band.index == bands[1].band.index && bands[1].band.index != bands[2].band.index) || (bands[1].band.index == bands[2].band.index && bands[2].band.index != bands[0].band.index)) {
				short naR = bands[0].band.getShortNA();
				short naB = bands[2].band.getShortNA();
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameR = processor.getShortFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameB = processor.getShortFrame(bands[2]);
				log.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRbShort(frameR, frameB, naR, naB, width, height, gamma, range, syncBands);			
			} else {
				short naR = bands[0].band.getShortNA();
				short naG = bands[1].band.getShortNA();
				short naB = bands[2].band.getShortNA();
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameR = processor.getShortFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameG = processor.getShortFrame(bands[1]);
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameB = processor.getShortFrame(bands[2]);
				//log.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRgbShort(frameR, frameG, frameB, naR, naG, naB, width, height, gamma, range, syncBands);				
			}			
		} else if(bands[0].band.isTypeFloatOrExactConvertible() && bands[1].band.isTypeFloatOrExactConvertible() && bands[2].band.isTypeFloatOrExactConvertible()) {
			if((bands[0].band.index == bands[1].band.index && bands[1].band.index != bands[2].band.index) || (bands[1].band.index == bands[2].band.index && bands[2].band.index != bands[0].band.index)) {
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameR = processor.getFloatFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameB = processor.getFloatFrame(bands[2]);
				//log.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRbFloat(frameR, frameB, width, height, gamma, range, syncBands);		
			} else {
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameR = processor.getFloatFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameG = processor.getFloatFrame(bands[1]);
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameB = processor.getFloatFrame(bands[2]);
				log.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRgbFloat(frameR, frameG, frameB, width, height, gamma, range, syncBands);
			}	
		} else {
			if((bands[0].band.index == bands[1].band.index && bands[1].band.index != bands[2].band.index) || (bands[1].band.index == bands[2].band.index && bands[2].band.index != bands[0].band.index)) {
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameR = processor.getDoubleFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameB = processor.getDoubleFrame(bands[2]);
				log.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRbDouble(frameR, frameB, width, height, gamma, range, syncBands);		
			} else {
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameR = processor.getDoubleFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameG = processor.getDoubleFrame(bands[1]);
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameB = processor.getDoubleFrame(bands[2]);
				log.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRgbDouble(frameR, frameG, frameB, width, height, gamma, range, syncBands);
			}	
		}
	}

	public static ImageBufferARGB rasterizeGrey(TimeBandProcessor processor, TimeBand timeband, int width, int height, double gamma, double[] range, Interruptor interruptor) {
		if(timeband.band.isTypeShortOrExactConvertible())  {
			short na = timeband.band.getShortNA();
			Interruptor.checkInterrupted(interruptor);
			ShortFrame frame = processor.getShortFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderGreyShort(frame, na, width, height, gamma, range);
		} else if(timeband.band.isTypeFloatOrExactConvertible())  {
			Interruptor.checkInterrupted(interruptor);
			FloatFrame frame = processor.getFloatFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderGreyFloat(frame, width, height, gamma, range);		
		} else {
			Interruptor.checkInterrupted(interruptor);
			DoubleFrame frame = processor.getDoubleFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderGreyDouble(frame, width, height, gamma, range);		
		}	
	}
	
	public static ImageBufferARGB rasterizePalette(TimeBandProcessor processor, TimeBand timeband, int width, int height, double gamma, double[] range, int[] palette, Interruptor interruptor) {
		if(timeband.band.isTypeShortOrExactConvertible())  {
			short na = timeband.band.getShortNA();
			Interruptor.checkInterrupted(interruptor);
			ShortFrame frame = processor.getShortFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderPaletteShort(frame, na, width, height, gamma, range, palette);
		} else if(timeband.band.isTypeFloatOrExactConvertible())  {
			Interruptor.checkInterrupted(interruptor);
			FloatFrame frame = processor.getFloatFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderPaletteFloat(frame, width, height, gamma, range, palette);		
		} else {
			Interruptor.checkInterrupted(interruptor);
			DoubleFrame frame = processor.getDoubleFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderPaletteDouble(frame, width, height, gamma, range, palette);		
		}	
	}
}
