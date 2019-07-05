package rasterdb.composite;

import rasterdb.Band;
import rasterdb.BandProcessor;
import rasterdb.tile.ProcessingDouble;
import util.Range2d;
import util.frame.DoubleFrame;

public class CompositeProcessor {	

	private final BandProcessor[] processors;
	
	public CompositeProcessor(CompositeRaster rasterMetaLayer, Range2d range2d, int timestamp, int reqWidth, int reqHeight) {
		processors = rasterMetaLayer.foreach(rasterdb -> new BandProcessor(rasterdb, range2d, timestamp, reqWidth, reqHeight), BandProcessor[]::new);
	}
	
	public DoubleFrame getDoubleFrame(Band band) {
		DoubleFrame doubleFrame = null;
		for(BandProcessor processor:processors) {
			if(processor.mayHavePixels(band)) {
				DoubleFrame levelDoubleFrame = processor.getDoubleFrame(band);
				if(doubleFrame == null) {
					doubleFrame = levelDoubleFrame;
				} else {
					ProcessingDouble.merge(doubleFrame.data, levelDoubleFrame.data);
				}
			}
		}
		if(doubleFrame == null) {
			doubleFrame = processors[0].getDoubleFrameConst(Double.NaN);
		}
		return doubleFrame;		
	}
}
