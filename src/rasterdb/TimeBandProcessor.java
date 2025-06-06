package rasterdb;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.tinylog.Logger;

import rasterdb.cell.CellInt16;
import rasterdb.cell.CellInt8;
import rasterdb.cell.CellType;
import rasterdb.cell.CellUint16;
import rasterdb.tile.Processing;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.ProcessingQuery;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TilePixel;
import rasterunit.BandKey;
import rasterunit.RasterUnitStorage;
import util.Range2d;
import util.frame.BooleanFrame;
import util.frame.ByteFrame;
import util.frame.CharFrame;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.IntFrame;
import util.frame.ShortFrame;

public class TimeBandProcessor implements TimeFrameProducer {

	public final RasterDB rasterdb;
	public final int tilePixelLen;
	public final Range2d range2d;	
	private int scale;

	private RasterUnitStorage pyramid_rasterUnit;
	private Range2d pyramid_srcRange;
	private Range2d pyramid_dstRange;
	private int pyramidLayerInternalDiv;
	private int pyramid;

	public TimeBandProcessor(RasterDB rasterdb, Range2d range2d) {
		this.rasterdb = rasterdb;
		this.tilePixelLen = rasterdb.getTilePixelLen();
		this.range2d = range2d;
		this.pyramid_rasterUnit = rasterdb.rasterUnit();
		this.pyramid_srcRange = range2d;
		this.pyramid_dstRange = range2d;
		this.pyramidLayerInternalDiv = 1;
		this.pyramid = 0;
		this.scale = 1;		
	}

	public TimeBandProcessor(RasterDB rasterdb, Range2d range2d, int scale) {
		this.rasterdb = rasterdb;
		this.tilePixelLen = rasterdb.getTilePixelLen();
		this.range2d = range2d;
		this.scale = scale;
		if(rasterdb.isInternalPyramid()) {
			//Logger.info("setScaleInternalPyramid");
			setScaleInternalPyramid(scale);
		} else {
			this.pyramid = 0;
			setScaleExternalPyramid(scale);
		}
	}

	public TimeBandProcessor(RasterDB rasterdb, Range2d range2d, int reqWidth, int reqHeight) {
		this(rasterdb, range2d, calcScale(range2d, reqWidth, reqHeight));
	}

	protected TimeBandProcessor(TimeBandProcessor tbp) {
		this.rasterdb = tbp.rasterdb;
		this.tilePixelLen = tbp.tilePixelLen;
		this.range2d = tbp.range2d;
		this.scale = tbp.scale;
		this.pyramid_rasterUnit = tbp.pyramid_rasterUnit;
		this.pyramid_srcRange = tbp.pyramid_srcRange;
		this.pyramid_dstRange = tbp.pyramid_dstRange;
		this.pyramidLayerInternalDiv = tbp.pyramidLayerInternalDiv;
		this.pyramid = tbp.pyramid;
	}
	
	public BandProcessor toBandProcessor(int timestamp) {
		return new BandProcessor(this, timestamp);
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
		//Logger.info("factor " + f);
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

	private void setScaleExternalPyramid(int scale) {
		//Logger.info("scale " + scale);
		switch(scale) {
		case 1:
			pyramid_rasterUnit = rasterdb.rasterUnit();
			pyramid_dstRange = range2d;
			pyramid_srcRange = range2d;
			pyramidLayerInternalDiv = 1;
			break;
		case 2:
			pyramid_rasterUnit = rasterdb.rasterUnit();
			pyramid_dstRange = range2d.floorDiv(2);
			pyramidLayerInternalDiv = 2;
			pyramid_srcRange = pyramid_dstRange.mulExpand(pyramidLayerInternalDiv);
			break;			
		case 4:
			pyramid_rasterUnit = rasterdb.rasterPyr1Unit();
			pyramid_dstRange = range2d.floorDiv(4);
			pyramid_srcRange = pyramid_dstRange;
			pyramidLayerInternalDiv = 1;
			break;
		case 8:
			pyramid_rasterUnit = rasterdb.rasterPyr1Unit();
			pyramid_dstRange = range2d.floorDiv(8);
			pyramid_srcRange = pyramid_dstRange.mulExpand(2);
			pyramidLayerInternalDiv = 2;
			break;			
		case 16:
			pyramid_rasterUnit = rasterdb.rasterPyr2Unit();
			pyramid_dstRange = range2d.floorDiv(16);
			pyramid_srcRange = pyramid_dstRange;
			pyramidLayerInternalDiv = 1;
			break;
		case 32:
			pyramid_rasterUnit = rasterdb.rasterPyr2Unit();
			pyramid_dstRange = range2d.floorDiv(32);
			pyramid_srcRange = pyramid_dstRange.mulExpand(2);
			pyramidLayerInternalDiv = 2;
			break;			
		case 64:
			pyramid_rasterUnit = rasterdb.rasterPyr3Unit();
			pyramid_dstRange = range2d.floorDiv(64);
			pyramid_srcRange = pyramid_dstRange;
			pyramidLayerInternalDiv = 1;
			break;
		case 128:
			pyramid_rasterUnit = rasterdb.rasterPyr3Unit();
			pyramid_dstRange = range2d.floorDiv(128);
			pyramid_srcRange = pyramid_dstRange.mulExpand(2);
			pyramidLayerInternalDiv = 2;
			break;			
		case 256:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(256);
			pyramid_srcRange = pyramid_dstRange;
			pyramidLayerInternalDiv = 1;
			break;
		case 512:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(512);
			pyramid_srcRange = pyramid_dstRange.mulExpand(2);
			pyramidLayerInternalDiv = 2;
			break;
		case 1024:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(1024);
			pyramid_srcRange = pyramid_dstRange.mulExpand(4);
			pyramidLayerInternalDiv = 4;
			break;
		case 2048:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(2048);
			pyramid_srcRange = pyramid_dstRange.mulExpand(8);
			pyramidLayerInternalDiv = 8;
			break;
		case 4096:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(4096);
			pyramid_srcRange = pyramid_dstRange.mulExpand(16);
			pyramidLayerInternalDiv = 16;
			break;
		case 8192:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(8192);
			pyramid_srcRange = pyramid_dstRange.mulExpand(32);
			pyramidLayerInternalDiv = 32;
			break;
		case 16384:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(16384);
			pyramid_srcRange = pyramid_dstRange.mulExpand(64);
			pyramidLayerInternalDiv = 64;
			break;
		case 32768:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(32768);
			pyramid_srcRange = pyramid_dstRange.mulExpand(128);
			pyramidLayerInternalDiv = 128;
			break;
		case 65536:
			pyramid_rasterUnit = rasterdb.rasterPyr4Unit();
			pyramid_dstRange = range2d.floorDiv(65536);
			pyramid_srcRange = pyramid_dstRange.mulExpand(256);
			pyramidLayerInternalDiv = 256;
			break;
		default:
			throw new RuntimeException("unknown scale " + scale);
		}
	}

	private void setScaleInternalPyramid(int scale) {
		int maxDiv = rasterdb.getTilePixelLen();
		int optimal_pyramid = 31 - Integer.numberOfLeadingZeros(scale);
		if(optimal_pyramid == 0) {
			pyramid_rasterUnit = rasterdb.rasterUnit();
			pyramid_dstRange = range2d;
			pyramid_srcRange = range2d;
			pyramidLayerInternalDiv = 1;
			pyramid = 0;
			this.scale = pyramidLayerInternalDiv;
		} else {
			pyramid_rasterUnit = rasterdb.rasterPyr1Unit();			
			int tmax = Processing.getTFromPyramidTimestamp(optimal_pyramid, Processing.TIMESTAMP_MAX);
			BandKey maxPyramidBandKey = pyramid_rasterUnit.bandKeysReadonly().floor(BandKey.toBandKeyMax(tmax));
			if(maxPyramidBandKey == null) {
				pyramid_rasterUnit = rasterdb.rasterUnit();
				pyramidLayerInternalDiv = scale > maxDiv ? maxDiv : scale;
				pyramid_dstRange = range2d.floorDiv(pyramidLayerInternalDiv);
				pyramid_srcRange = pyramid_dstRange.mulExpand(pyramidLayerInternalDiv);
				pyramid = 0;
				this.scale = pyramidLayerInternalDiv;
			} else {
				//Logger.info("max pyramid " + Processing.getPyramidFromT(pyramid_rasterUnit.bandKeysReadonly().last().t));
				pyramid = Processing.getPyramidFromT(maxPyramidBandKey.t);
				int toPyramidDiv = 1 << pyramid; 
				int div = scale >>> pyramid;
				pyramidLayerInternalDiv = div > maxDiv ? maxDiv : div;
				this.scale = pyramidLayerInternalDiv << pyramid; 
				pyramid_dstRange = range2d.floorDiv(toPyramidDiv).floorDiv(pyramidLayerInternalDiv);
				pyramid_srcRange = pyramid_dstRange.mulExpand(pyramidLayerInternalDiv);
				Logger.info("scale " + scale + "  toPyramidDiv " + toPyramidDiv +  "  pyramid " + pyramid + "  pyramidLayerInternalDiv " + pyramidLayerInternalDiv  + " ->scale " + scale);				
			}
		}		
	}

	private short[][] readInt16(int timestamp, Band band) {
		CellInt16 cellInt16 = new CellInt16(rasterdb.getTilePixelLen());
		int t = Processing.getTFromPyramidTimestamp(pyramid, timestamp);
		return cellInt16.read(pyramid_rasterUnit, t, band, pyramid_srcRange, pyramidLayerInternalDiv);	
	}
	
	private char[][] readUint16(int timestamp, Band band) {
		CellUint16 cellUint16 = new CellUint16(rasterdb.getTilePixelLen());
		int t = Processing.getTFromPyramidTimestamp(pyramid, timestamp);
		return cellUint16.read(pyramid_rasterUnit, t, band, pyramid_srcRange, pyramidLayerInternalDiv);	
	}

	private byte[][] readInt8(int timestamp, Band band) {
		CellInt8 cellInt8 = new CellInt8(rasterdb.getTilePixelLen());
		int t = Processing.getTFromPyramidTimestamp(pyramid, timestamp);
		return cellInt8.read(pyramid_rasterUnit, t, band, pyramid_srcRange, pyramidLayerInternalDiv);	
	}


	private short[][] readShort(int timestamp, Band band) {
		//Logger.info("get from pyramid " + pyramid + "   div " + pyramidDiv);
		//Logger.info("src " + pyramid_srcRange);
		//Logger.info("src " + pyramid_dstRange);
		int t = Processing.getTFromPyramidTimestamp(pyramid, timestamp);
		return ProcessingShort.readPixels(pyramidLayerInternalDiv, pyramid_rasterUnit, t, band, pyramid_srcRange);		
	}

	private short[][] readShort(TimeBand timeband) {
		return readShort(timeband.timestamp, timeband.band);	
	}

	private float[][] readFloat(int timestamp, Band band) {
		int t = Processing.getTFromPyramidTimestamp(pyramid, timestamp);
		return ProcessingFloat.readPixels(pyramidLayerInternalDiv, pyramid_rasterUnit, t, band, pyramid_srcRange);	
	}

	private float[][] readFloat(TimeBand timeband) {
		return readFloat(timeband.timestamp, timeband.band);	
	}
	
	public IntFrame getIntFrame(TimeBand timeband) {
		return getIntFrame(timeband.timestamp, timeband.band);
	}
	
	public IntFrame getIntFrame(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			short na_src = 0;
			int na_target = 0;	
			return IntFrame.ofShorts(ShortFrame.of(readShort(timestamp, band), range2d), na_src, na_target);
		}
		case TilePixel.TYPE_FLOAT: {
			Logger.warn("downcast float to short");
			short na_target = 0;
			return IntFrame.ofFloats(FloatFrame.of(readFloat(timestamp, band), range2d), na_target);
		}
		case CellType.INT16: {
			short na_src = 0;
			int na_target = 0;	
			return IntFrame.ofShorts(ShortFrame.of(readInt16(timestamp, band), range2d), na_src, na_target);
		}
		case CellType.UINT16: {
			char na_src = 0;
			int na_target = 0;	
			return IntFrame.ofChars(CharFrame.of(readUint16(timestamp, band), range2d), na_src, na_target);
		}
		case CellType.INT8: {
			byte na_src = 0;
			int na_target = 0;	
			return IntFrame.ofBytes(ByteFrame.of(readInt8(timestamp, band), range2d), na_src, na_target);
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
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
			Logger.warn("downcast float to short");
			short na_target = 0;
			return ShortFrame.ofFloats(FloatFrame.of(readFloat(timestamp, band), range2d), na_target);
		}
		case CellType.INT16: {
			return ShortFrame.of(readInt16(timestamp, band), range2d);
		}
		case CellType.UINT16: {
			Logger.warn("downcast uint16 to int16");
			char na_src = 0;
			short na_target = 0;
			return ShortFrame.ofChars(CharFrame.of(readUint16(timestamp, band), range2d), na_src, na_target);
		}
		case CellType.INT8: {
			byte na_src = 0;
			short na_target = 0;	
			return ShortFrame.ofBytes(ByteFrame.of(readInt8(timestamp, band), range2d), na_src, na_target);
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
	}
	
	public CharFrame getCharFrame(TimeBand timeband) {
		return getCharFrame(timeband.timestamp, timeband.band);
	}
	
	public CharFrame getCharFrame(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			Logger.warn("cast short to char");
			short na_src = 0;
			char na_dst = 0;	
			return CharFrame.ofShorts(ShortFrame.of(readShort(timestamp, band), range2d), na_src, na_dst);
		}
		case TilePixel.TYPE_FLOAT: {
			Logger.warn("downcast float to char");
			char na_target = 0;
			return CharFrame.ofFloats(FloatFrame.of(readFloat(timestamp, band), range2d), na_target);
		}
		case CellType.INT16: {
			Logger.warn("cast int16 to char");
			short na_src = 0;
			char na_dst = 0;	
			return CharFrame.ofShorts(ShortFrame.of(readInt16(timestamp, band), range2d), na_src, na_dst);
		}
		case CellType.INT8: {
			Logger.warn("cast int8 to char");
			byte na_src = 0;
			char na_dst = 0;	
			return CharFrame.ofBytes(ByteFrame.of(readInt8(timestamp, band), range2d), na_src, na_dst);
		}
		case CellType.UINT16: {
			return CharFrame.of(readUint16(timestamp, band), range2d);
		}
		default:
			throw new RuntimeException("unknown tile type: "+tileType);
		}
	}

	public ByteFrame getByteFrame(TimeBand timeband) {
		return getByteFrame(timeband.timestamp, timeband.band);
	}

	public ByteFrame getByteFrame(int timestamp, Band band) {
		int tileType = band.type;
		switch(tileType) {
		case TilePixel.TYPE_SHORT: {
			Logger.warn("downcast short to byte");
			short na_src = 0;
			byte na_target = 0;			
			return ByteFrame.ofShorts(ShortFrame.of(readShort(timestamp, band), range2d), na_src, na_target);
		}
		case TilePixel.TYPE_FLOAT: {
			Logger.warn("downcast float to byte");
			byte na_target = 0;
			return ByteFrame.ofFloats(FloatFrame.of(readFloat(timestamp, band), range2d), na_target);
		}
		case CellType.INT16: {
			Logger.warn("downcast short to byte");
			short na_src = 0;
			byte na_target = 0;
			return ByteFrame.ofShorts(ShortFrame.of(readInt16(timestamp, band), range2d), na_src, na_target);
		}
		case CellType.UINT16: {
			Logger.warn("downcast uint16 to byte");
			char na_src = 0;
			byte na_target = 0;
			return ByteFrame.ofChars(CharFrame.of(readUint16(timestamp, band), range2d), na_src, na_target);
		}
		case CellType.INT8: {
			return ByteFrame.of(readInt8(timestamp, band), range2d);
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
		case TilePixel.TYPE_SHORT:
		case CellType.INT16: {
			short na = band.getInt16NA();			
			return FloatFrame.ofShortsWithNA(getShortFrame(timestamp, band), na);
		}
		case CellType.INT8: {
			byte na = band.getInt8NA();			
			return FloatFrame.ofShortsWithNA(getShortFrame(timestamp, band), na);
		}
		case TilePixel.TYPE_FLOAT: {			
			return FloatFrame.of(readFloat(timestamp, band), range2d);
		}
		case CellType.UINT16: {
			char na = band.getUint16NA();			
			return FloatFrame.ofCharsWithNA(getCharFrame(timestamp, band), na);
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
		case TilePixel.TYPE_SHORT:
		case CellType.INT16: {
			short na = band.getInt16NA();			
			return DoubleFrame.ofShortsWithNA(getShortFrame(timestamp, band), na);
		}
		case CellType.INT8: {
			byte na = band.getInt8NA();			
			return DoubleFrame.ofBytesWithNA(getByteFrame(timestamp, band), na);
		}
		case TilePixel.TYPE_FLOAT: {			
			return DoubleFrame.ofFloats(readFloat(timestamp, band), range2d);
		}
		case CellType.UINT16: {
			char na = band.getUint16NA();			
			return DoubleFrame.ofCharsWithNA(getCharFrame(timestamp, band), na);
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
		case TilePixel.TYPE_SHORT:
		case CellType.INT16: {
			short na = band.getInt16NA();
			ShortFrame shortFrame = getShortFrame(timestamp, band);
			return shortFrame.toMask(na);
		}
		case CellType.INT8: {
			byte na = band.getInt8NA();
			ByteFrame byteFrame = getByteFrame(timestamp, band);
			return byteFrame.toMask(na);
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
		int t = Processing.getTFromPyramidTimestamp(pyramid, timestamp);
		return ProcessingQuery.mayHavePixels(pyramid_rasterUnit, t, band, pyramid_srcRange);
	}

	public DoubleFrame getDoubleFrameConst(double value) {		
		DoubleFrame doubleFrame = DoubleFrame.ofRange2d(pyramid_dstRange.getWidth(), pyramid_dstRange.getHeight(), range2d);
		doubleFrame.fill(value);
		return doubleFrame;
	}	

	public Collection<Band> getBands() {
		return rasterdb.bandMapReadonly.values();		
	}

	public List<TimeBand> getTimeBands(int timestamp) {
		return toTimeBands(timestamp, getBands());
	}

	public Band getBand(int index) {
		return rasterdb.bandMapReadonly.get(index);		
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
	
	public TimeBand toTimeBand(int timestamp, Band band) {
		return new TimeBand(timestamp, band);	
	}

	public List<TimeBand> toTimeBands(int timestamp, Band[] bands) {
		return TimeBand.of(timestamp, Arrays.stream(bands));
	}

	public List<TimeBand> toTimeBands(int timestamp, Collection<Band> bands) {
		return TimeBand.of(timestamp, bands.stream());
	}
}
