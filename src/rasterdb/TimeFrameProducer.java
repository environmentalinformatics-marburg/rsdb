package rasterdb;

import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public interface TimeFrameProducer {

	ShortFrame getShortFrame(int timestamp, Band band);
	FloatFrame getFloatFrame(int timestamp, Band band);
	DoubleFrame getDoubleFrame(int timestamp, Band band);
	DoubleFrame getDoubleFrameConst(double value);
	Band getBand(int index);
	TimeBand getTimeBand(int timestamp, int bandIndex);

	
	default ShortFrame getShortFrame(TimeBand timeband)  {
		return getShortFrame(timeband.timestamp, timeband.band);
	}
	
	default FloatFrame getFloatFrame(TimeBand timeband)  {
		return getFloatFrame(timeband.timestamp, timeband.band);
	}
	
	default DoubleFrame getDoubleFrame(TimeBand timeband)  {
		return getDoubleFrame(timeband.timestamp, timeband.band);
	}
}
