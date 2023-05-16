package rasterdb;


import org.tinylog.Logger;

import server.api.rasterdb.WmsHandler.Interruptor;
import util.Timer;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;
import util.image.ImageBufferARGB;
import util.image.Renderer;

public class Rasterizer {	
	
	public static ImageBufferARGB rasterizeRGB(TimeFrameProducer processor, RasterDB rasterdb, int timestamp, int width, int height, double gamma, double[] range, boolean syncBands, Interruptor interruptor) {
		TimeBand[] bands = TimeBand.of(timestamp, BandProcessing.getBestColorBands(rasterdb));
		return rasterizeRGB(processor, bands, width, height, gamma, range, syncBands, interruptor);
	}

	public static ImageBufferARGB rasterizeRGB(TimeFrameProducer processor, TimeBand[] bands, int width, int height, double gamma, double[] range, boolean syncBands, Interruptor interruptor) {
		Timer.start("load");
		if(bands[0].band.isPixelTypeInt16OrExactConvertible() && bands[1].band.isPixelTypeInt16OrExactConvertible() && bands[2].band.isPixelTypeInt16OrExactConvertible()) {
			if((bands[0].band.index == bands[1].band.index && bands[1].band.index != bands[2].band.index) || (bands[1].band.index == bands[2].band.index && bands[2].band.index != bands[0].band.index)) {
				short naR = bands[0].band.getInt16NA();
				short naB = bands[2].band.getInt16NA();
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameR = processor.getShortFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameB = processor.getShortFrame(bands[2]);
				Logger.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRbShort(frameR, frameB, naR, naB, width, height, gamma, range, syncBands);			
			} else {
				short naR = bands[0].band.getInt16NA();
				short naG = bands[1].band.getInt16NA();
				short naB = bands[2].band.getInt16NA();
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameR = processor.getShortFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameG = processor.getShortFrame(bands[1]);
				Interruptor.checkInterrupted(interruptor);
				ShortFrame frameB = processor.getShortFrame(bands[2]);
				//Logger.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRgbShort(frameR, frameG, frameB, naR, naG, naB, width, height, gamma, range, syncBands);				
			}			
		} else if(bands[0].band.isPixelTypeFloat32OrExactConvertible() && bands[1].band.isPixelTypeFloat32OrExactConvertible() && bands[2].band.isPixelTypeFloat32OrExactConvertible()) {
			if((bands[0].band.index == bands[1].band.index && bands[1].band.index != bands[2].band.index) || (bands[1].band.index == bands[2].band.index && bands[2].band.index != bands[0].band.index)) {
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameR = processor.getFloatFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameB = processor.getFloatFrame(bands[2]);
				//Logger.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRbFloat(frameR, frameB, width, height, gamma, range, syncBands);		
			} else {
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameR = processor.getFloatFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameG = processor.getFloatFrame(bands[1]);
				Interruptor.checkInterrupted(interruptor);
				FloatFrame frameB = processor.getFloatFrame(bands[2]);
				Logger.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRgbFloat(frameR, frameG, frameB, width, height, gamma, range, syncBands);
			}	
		} else {
			if((bands[0].band.index == bands[1].band.index && bands[1].band.index != bands[2].band.index) || (bands[1].band.index == bands[2].band.index && bands[2].band.index != bands[0].band.index)) {
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameR = processor.getDoubleFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameB = processor.getDoubleFrame(bands[2]);
				Logger.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRbDouble(frameR, frameB, width, height, gamma, range, syncBands);		
			} else {
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameR = processor.getDoubleFrame(bands[0]);
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameG = processor.getDoubleFrame(bands[1]);
				Interruptor.checkInterrupted(interruptor);
				DoubleFrame frameB = processor.getDoubleFrame(bands[2]);
				Logger.info(Timer.stop("load"));
				Interruptor.checkInterrupted(interruptor);
				return Renderer.renderRgbDouble(frameR, frameG, frameB, width, height, gamma, range, syncBands);
			}	
		}
	}

	public static ImageBufferARGB rasterizeGrey(TimeFrameProducer processor, TimeBand timeband, int width, int height, double gamma, double[] range, Interruptor interruptor) {
		if(timeband.band.isPixelTypeInt16OrExactConvertible())  {
			short na = timeband.band.getInt16NA();
			Interruptor.checkInterrupted(interruptor);
			ShortFrame frame = processor.getShortFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderGreyShort(frame, na, width, height, gamma, range);
		} else if(timeband.band.isPixelTypeFloat32OrExactConvertible())  {
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
	
	public static ImageBufferARGB rasterizePalette(TimeFrameProducer processor, TimeBand timeband, int width, int height, double gamma, double[] range, int[] palette, Interruptor interruptor) {
		if(timeband.band.isPixelTypeInt16OrExactConvertible())  {
			short na = timeband.band.getInt16NA();
			Interruptor.checkInterrupted(interruptor);
			ShortFrame frame = processor.getShortFrame(timeband);
			Interruptor.checkInterrupted(interruptor);
			return Renderer.renderPaletteShort(frame, na, width, height, gamma, range, palette);
		} else if(timeband.band.isPixelTypeFloat32OrExactConvertible())  {
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
