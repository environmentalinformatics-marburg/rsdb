package rasterdb;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterunit.RasterUnit;
import util.Range2d;
import util.frame.BooleanFrame;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public class TimeBandProcessor {
	static final Logger log = LogManager.getLogger();

	public final RasterDB rasterdb;
	public final Range2d range2d;
	private final int scale;
	
	private RasterUnit pyramid_rasterUnit;
	private Range2d pyramid_srcRange;
	private Range2d pyramid_dstRange;
	int pyramidDiv;


	public TimeBandProcessor(RasterDB rasterdb, Range2d range2d) {
		this.rasterdb = rasterdb;
		this.range2d = range2d;
		this.pyramid_rasterUnit = rasterdb.rasterUnit();
		this.pyramid_srcRange = range2d;
		this.pyramid_dstRange = range2d;
		this.pyramidDiv = 1;
		this.scale = 1;
	}

	public TimeBandProcessor(RasterDB rasterdb, Range2d range2d, int scale) {
		this.rasterdb = rasterdb;
		this.range2d = range2d;
		this.scale = scale;
		setScale(scale);
	}

	public TimeBandProcessor(RasterDB rasterdb, Range2d range2d, int reqWidth, int reqHeight) {
		this(rasterdb, range2d, calcScale(range2d, reqWidth, reqHeight));
	}
	
	public static int calcScale(Range2d range2d, int reqWidth, int reqHeight) {
		return factorToScale(calcFactor(range2d.getWidth(), range2d.getHeight(), reqWidth, reqHeight));
	}
	
	private static int calcFactor(int srcWidth, int srcHeight, int reqWidth, int reqHeight) {
		if(reqWidth > 0) {
			if(reqHeight > 0) {
				return Math.min(srcWidth / reqWidth, srcHeight / reqHeight); // get lower factor for better quality
			} else {
				return srcWidth / reqWidth;
			}
		} else {
			if(reqHeight > 0) {
				return srcHeight / reqHeight;
			} else {
				return 1;
			}
		}
	}

	private static int factorToScale(int f) {
		log.info("factor " + f);
		if(f >= 65536) return 65536;
		if(f >= 32768) return 32768;
		if(f >= 16384) return 16384;
		if(f >= 8192) return 8192;
		if(f >= 4096) return 4096;
		if(f >= 2048) return 2048;
		if(f >= 1024) return 1024;
		if(f >= 512) return 512;
		if(f >= 256) return 256;
		if(f >= 128) return 128;
		if(f >= 64) return 64;
		if(f >= 32) return 32;
		if(f >= 16) return 16;
		if(f >= 8) return 8;
		if(f >= 4) return 4;
		if(f >= 2) return 2;
		return 1;
	}

	private void setScale(int scale) {
		switch(scale) {
		case 1:
			pyramid_rasterUnit = rasterdb.rasterUnit();
			pyramid_dstRange = range2d;
			pyramid_srcRange = range2d;
			pyramidDiv = 1;
			break;
		case 2:
			pyramid_rasterUnit = rasterdb.rasterUnit();
			pyramid_dstRange = range2d.floorDiv(2);
			pyramid_srcRange = pyramid_dstRange.mul(2);
			pyramidDiv = 2;
			break;			
		case 4:
			pyramid_rasterUnit = rasterdb.rasterPyr1Unit();
			pyramid_dstRange = range2d.floorDiv(4);
			pyramid_srcRange = pyramid_dstRange;
			pyramidDiv = 1;
			break;
		case 8:
			pyramid_rasterUnit = rasterdb.rasterPyr1Unit();
			pyramid_dstRange = range2d.floorDiv(8);
			pyramid_srcRange = pyramid_dstRange.mul(2);
			pyramidDiv = 2;
			break;			
		case 16:
			pyramid_rasterUnit = rasterdb.rasterPyr2Unit();
			pyramid_dstRange = range2d.floorDiv(16);
			pyramid_srcRange = pyramid_dstRange;
			pyramidDiv = 1;
			break;
		case 32:
			pyramid_rasterUnit = rasterdb.rasterPyr2Unit();
			pyramid_dstRange = range2d.floorDiv(32);
			pyramid_srcRange = pyramid_dstRange.mul(2);
			pyramidDiv = 2;
			break;			
		case 64:
			pyramid_rasterUnit = rasterdb.rasterPyr3Unit();
			pyramid_dstRange = range2d.floorDiv(64);
			pyramid_srcRange = pyramid_dstRange;
			pyramidDiv = 1;
			break;
		case 128:
			pyramid_rasterUnit = rasterdb.rasterPyr3Unit();
			pyramid_dstRange = range2d.floorDiv(128);
			pyramid_srcRange = pyramid_dstRange.mul(2);
			pyramidDiv = 2;
			break;			
		case 256:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(256);
			pyramid_srcRange = pyramid_dstRange;
			pyramidDiv = 1;
			break;
		case 512:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(512);
			pyramid_srcRange = pyramid_dstRange.mul(2);
			pyramidDiv = 2;
			break;
		case 1024:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(1024);
			pyramid_srcRange = pyramid_dstRange.mul(4);
			pyramidDiv = 4;
			break;
		case 2048:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(2048);
			pyramid_srcRange = pyramid_dstRange.mul(8);
			pyramidDiv = 8;
			break;
		case 4096:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(4096);
			pyramid_srcRange = pyramid_dstRange.mul(16);
			pyramidDiv = 16;
			break;
		case 8192:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(8192);
			pyramid_srcRange = pyramid_dstRange.mul(32);
			pyramidDiv = 32;
			break;
		case 16384:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(16384);
			pyramid_srcRange = pyramid_dstRange.mul(64);
			pyramidDiv = 64;
			break;
		case 32768:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(32768);
			pyramid_srcRange = pyramid_dstRange.mul(128);
			pyramidDiv = 128;
			break;
		case 65536:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(65536);
			pyramid_srcRange = pyramid_dstRange.mul(256);
			pyramidDiv = 256;
			break;
		default:
			throw new RuntimeException("unknown scale " + scale);
		}
	}
	
	
	private short[][] readShort(int timestamp, Band band) {
		return ProcessingShort.readPixels(pyramidDiv, pyramid_rasterUnit, timestamp, band, pyramid_srcRange);		
	}
	
	private short[][] readShort(TimeBand timeband) {
		return readShort(timeband.timestamp, timeband.band);	
	}
	
	private float[][] readFloat(int timestamp, Band band) {
		return ProcessingFloat.readPixels(pyramidDiv, pyramid_rasterUnit, timestamp, band, pyramid_srcRange);	
	}
	
	private float[][] readFloat(TimeBand timeband) {
		return readFloat(timeband.timestamp, timeband.band);	
	}
	
	public ShortFrame getShortFrame(TimeBand timeband) {
		return getShortFrame(timeband.timestamp, timeband.band);
	}

	public ShortFrame getShortFrame(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			return ShortFrame.of(readShort(timestamp, band), range2d);
		}
		case TilePixel.TYPE_FLOAT: {
			log.warn("downcast float to short");
			short na_target = 0;
			return ShortFrame.ofFloats(FloatFrame.of(readFloat(timestamp, band), range2d), na_target);
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
	}
	
	public FloatFrame getFloatFrame(TimeBand timeband) {
		return getFloatFrame(timeband.timestamp, timeband.band);
	}

	public FloatFrame getFloatFrame(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			short na = band.getShortNA();			
			return FloatFrame.ofShortsWithNA(ShortFrame.of(readShort(timestamp, band), range2d), na);
		}
		case TilePixel.TYPE_FLOAT: {			
			return FloatFrame.of(readFloat(timestamp, band), range2d);
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
	}

	public DoubleFrame getDoubleFrame(TimeBand timeband) {
		return getDoubleFrame(timeband.timestamp, timeband.band);
	}

	public DoubleFrame getDoubleFrame(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			short na = band.getShortNA();			
			return DoubleFrame.ofShortsWithNA(ShortFrame.of(readShort(timestamp, band), range2d), na);
		}
		case TilePixel.TYPE_FLOAT: {			
			return DoubleFrame.ofFloats(readFloat(timestamp, band), range2d);
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
	}

	public BooleanFrame getMask(TimeBand timeband) {
		return getMask(timeband.timestamp, timeband.band);
	}
	
	public BooleanFrame getMask(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			short na = band.getShortNA();
			ShortFrame shortFrame = getShortFrame(timestamp, band);
			return shortFrame.toMask(na);
		}
		case TilePixel.TYPE_FLOAT: {
			FloatFrame floatFrame = getFloatFrame(timestamp, band);
			return floatFrame.toMask();
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
	}
	
	public boolean mayHavePixels(TimeBand timeband) {	
		return mayHavePixels(timeband.timestamp, timeband.band);
	}
	
	public boolean mayHavePixels(int timestamp, Band band) {		
		return ProcessingQuery.mayHavePixels(pyramid_rasterUnit, timestamp, band, pyramid_srcRange);
	}

	public DoubleFrame getDoubleFrameConst(double value) {		
		DoubleFrame doubleFrame = DoubleFrame.ofRange2d(pyramid_dstRange.getWidth(), pyramid_dstRange.getHeight(), range2d);
		doubleFrame.fill(value);
		return doubleFrame;
	}	

	public Collection<Band> getBands() {
		return rasterdb.bandMap.values();		
	}
	
	public List<TimeBand> getTimeBands(int timestamp) {
		return toTimeBands(timestamp, getBands());
	}

	public Band getBand(int index) {
		return rasterdb.bandMap.get(index);		
	}
	
	public TimeBand getTimeBand(int timestamp, int bandIndex) {
		Band band = getBand(bandIndex);		
		return band == null ? null : new TimeBand(timestamp, band);		
	}
	
	public Range2d getSrcRange() {
		return pyramid_srcRange;
	}

	public Range2d getDstRange() {
		return pyramid_dstRange;
	}
	
	public int getScale() {
		return scale;
	}
	
	public List<TimeBand> toTimeBands(int timestamp, Band[] bands) {
		return TimeBand.of(timestamp, Arrays.stream(bands));
	}
	
	public List<TimeBand> toTimeBands(int timestamp, Collection<Band> bands) {
		return TimeBand.of(timestamp, bands.stream());
	}
}
