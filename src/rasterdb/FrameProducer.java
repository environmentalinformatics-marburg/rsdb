package rasterdb;

import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public interface FrameProducer {
	
	ShortFrame getShortFrame(TimeBand timeband);
	FloatFrame getFloatFrame(TimeBand timeband);
	DoubleFrame getDoubleFrame(TimeBand timeband);
	
}
