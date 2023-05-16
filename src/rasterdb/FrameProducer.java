package rasterdb;

import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public interface FrameProducer extends TimeFrameProducer {
	
	ShortFrame getShortFrame(Band band);	
	FloatFrame getFloatFrame(Band band);
	DoubleFrame getDoubleFrame(Band band);
	public TimeBand getTimeBand(int bandIndex);
}
