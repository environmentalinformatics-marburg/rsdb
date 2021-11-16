package rasterdb;

import java.util.List;

import util.Range2d;
import util.frame.BooleanFrame;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public class BandProcessor extends TimeBandProcessor {
	public final int timestamp;
	
	public BandProcessor(RasterDB rasterdb, Range2d range2d, int timestamp) {
		super(rasterdb, range2d);
		this.timestamp = timestamp;
	}

	public BandProcessor(RasterDB rasterdb, Range2d range2d, int timestamp, int scale) {
		super(rasterdb, range2d, scale);
		this.timestamp = timestamp;
		//log.info("resulting scale " + scale);
	}

	public BandProcessor(RasterDB rasterdb, Range2d range2d, int timestamp, int reqWidth, int reqHeight) {
		super(rasterdb, range2d, reqWidth, reqHeight);
		this.timestamp = timestamp;
	}	
	
	public ShortFrame getShortFrame(Band band) {
		return getShortFrame(timestamp, band);
	}
	
	public FloatFrame getFloatFrame(Band band) {
		return getFloatFrame(timestamp, band);
	}

	public DoubleFrame getDoubleFrame(Band band) {
		return getDoubleFrame(timestamp, band);
	}

	public BooleanFrame getMask(Band band) {
		return getMask(timestamp, band);
	}	
	
	public boolean mayHavePixels(Band band) {	
		return mayHavePixels(timestamp, band);
	}
	
	public TimeBand getTimeBand(int bandIndex) {	
		return getTimeBand(timestamp, bandIndex);		
	}
	
	public List<TimeBand> getTimeBands() {
		return getTimeBands(timestamp);
	}
	
	public List<TimeBand> toTimeBands(Band[] bands) {
		return toTimeBands(timestamp, bands);
	}
}
