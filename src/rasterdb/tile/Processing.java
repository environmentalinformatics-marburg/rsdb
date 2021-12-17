package rasterdb.tile;

import java.io.IOException;
import java.util.NavigableSet;


import org.tinylog.Logger;

import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellInt16;
import rasterdb.cell.CellType;
import rasterunit.BandKey;
import rasterunit.RasterUnitStorage;
import util.Range2d;

public class Processing {
		

	public static class Commiter {
		public static final int maxTileWriteCount = 256;
		private final RasterUnitStorage targetUnit;
		private int tileWriteCount = 0;
		private long totalWriteCount = 0;

		public Commiter(RasterUnitStorage targetUnit) {
			this.targetUnit = targetUnit;
		}

		public void add(int tilesWrittenInBand) {
			tileWriteCount += tilesWrittenInBand;
			checkCommit();
		}

		private void checkCommit() {
			if(maxTileWriteCount <= tileWriteCount) {
				Logger.info("commit");
				targetUnit.commit();
				totalWriteCount += tileWriteCount;
				tileWriteCount = 0;
			}
		}

		public void checkFinishCommit() {
			if(0 < tileWriteCount) {
				Logger.info("commit");
				targetUnit.commit();
				totalWriteCount += tileWriteCount;
				tileWriteCount = 0;
			}
		}

		public long getTotalWriteCount() {
			return totalWriteCount;
		}
	}

	public static long writeStorageDiv(RasterDB rasterdb, RasterUnitStorage srcStorage, RasterUnitStorage dstStorage, int div) throws IOException {
		Commiter commiter = new Commiter(dstStorage);
		for(BandKey srcBandKey:srcStorage.bandKeysReadonly()) {
			BandKey dstBandKey = srcBandKey;
			Band band = rasterdb.bandMapReadonly.get(srcBandKey.b);
			writeStorageBandDiv(rasterdb, band, div, srcStorage, srcBandKey, dstStorage, dstBandKey, commiter);
		}		
		commiter.checkFinishCommit();
		return commiter.getTotalWriteCount();
	}


	public static void writeStorageBandDiv(RasterDB rasterdb, Band band, int div, RasterUnitStorage srcStorage, BandKey srcBandKey, RasterUnitStorage dstStorage, BandKey dstBandKey, Commiter commiter) throws IOException {
		switch (band.type) {
		case TilePixel.TYPE_SHORT:
			ProcessingShort.writeStorageBandDiv(band, div, srcStorage, srcBandKey, dstStorage, dstBandKey, commiter);				
			break;
		case TilePixel.TYPE_FLOAT:
			ProcessingFloat.writeStorageBandDiv(band, div, srcStorage, srcBandKey, dstStorage, dstBandKey, commiter);				
			break;
		case CellType.INT16:
			CellInt16 cellInt16 = new CellInt16(rasterdb.getTilePixelLen());
			cellInt16.writeStorageBandDiv(band, div, srcStorage, srcBandKey, dstStorage, dstBandKey, commiter);	
			break;
		default:
			throw new RuntimeException("unknown band type");
		}
	}


	public static long rebuildPyramid(RasterDB rasterdb, RasterUnitStorage srcStorage, RasterUnitStorage dstStorage, int div) throws IOException {
		Commiter commiter = new Commiter(dstStorage);
		rebuildPyramid(rasterdb, srcStorage, 0, dstStorage, 1, div, commiter);
		commiter.checkFinishCommit();
		return commiter.getTotalWriteCount();
	}

	public static int getTimestampFromT(int t) {
		return t & 0xfffffff;
	}

	public static int getPyramidFromT(int t) {
		return t >>> 28;
	}

	public static int getTFromPyramidTimestamp(int pyramid, int timestamp) {
		if((0xfffffff0 & pyramid) != 0) {
			throw new RuntimeException("invalid pyramid number: " + pyramid);
		}
		if((0xf0000000 & timestamp) != 0) {
			throw new RuntimeException("invalid timestamp number: " + timestamp);
		}
		return (pyramid << 28) | timestamp;
	}

	public static final int PYRAMID_MIN = 0x0;
	public static final int PYRAMID_MAX = 0xf;
	public static final int TIMESTAMP_MIN = 0x00000000;
	public static final int TIMESTAMP_MAX = 0x0fffffff;


	public static void rebuildPyramid(RasterDB rasterdb, RasterUnitStorage srcStorage, int srcPyramid, RasterUnitStorage dstStorage, int dstPyramid, int div, Commiter commiter) throws IOException {

		int tmin = getTFromPyramidTimestamp(srcPyramid, TIMESTAMP_MIN);
		int tmax = getTFromPyramidTimestamp(srcPyramid, TIMESTAMP_MAX);
		NavigableSet<BandKey> srcBandKeys = srcStorage.bandKeysReadonly().subSet(BandKey.toBandKeyMin(tmin), true, BandKey.toBandKeyMax(tmax), true);

		boolean needProcessing = false;
		for(BandKey srcBandKey : srcBandKeys) {
			Range2d range = srcStorage.getTileRange2d(srcBandKey);
			if(range != null && ( range.getWidth() > 2 || range.getHeight() > 2)) {
				Logger.info("pyramid " + srcPyramid + " size " + range);
				needProcessing = true;
				break;
			}
		}

		if(needProcessing) {
			if(dstPyramid > PYRAMID_MAX) {
				Logger.info("process pyramid limit reached" + dstPyramid + " -> down scale " + Math.pow(2, dstPyramid) + " processing stopped.");
				return;
			}
			Logger.info("process pyramid " + dstPyramid + " -> down scale " + Math.pow(2, dstPyramid));
			for(BandKey srcBandKey : srcBandKeys) {
				Band band = rasterdb.bandMapReadonly.get(srcBandKey.b);
				int srcTimestamp = getTimestampFromT(srcBandKey.t); 
				int dstT = getTFromPyramidTimestamp(dstPyramid, srcTimestamp);
				BandKey dstBandKey = new BandKey(dstT, srcBandKey.b);
				writeStorageBandDiv(rasterdb, band, div, srcStorage, srcBandKey, dstStorage, dstBandKey , commiter);
			}

			rebuildPyramid(rasterdb, dstStorage, dstPyramid, dstStorage, dstPyramid + 1, div, commiter);
		}
	}
}
