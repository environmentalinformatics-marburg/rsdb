package rasterdb;

import pointcloud.Rect2d;
import server.api.rasterdb.TimeFrameReprojector;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public class FrameReprojector extends TimeFrameReprojector implements FrameProducer {

	public final int timestamp;

	public FrameReprojector(RasterDB rasterdb, int timestamp, int layerEPSG, int wmsEPSG, Rect2d wmsRect, int width, int height) {
		super(rasterdb, layerEPSG, wmsEPSG, wmsRect, width, height);
		this.timestamp = timestamp;
	}

	@Override
	public ShortFrame getShortFrame(Band band) {
		return getShortFrame(timestamp, band);
	}

	@Override
	public FloatFrame getFloatFrame(Band band) {
		return getFloatFrame(timestamp, band);
	}

	@Override
	public DoubleFrame getDoubleFrame(Band band) {
		return getDoubleFrame(timestamp, band);
	}
	
	@Override
	public TimeBand getTimeBand(int bandIndex) {	
		return getTimeBand(timestamp, bandIndex);		
	}
}
