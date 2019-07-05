package rasterdb.tile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.RasterDB;
import rasterunit.BandKey;
import rasterunit.RasterUnit;

public class Processing {
	private static final Logger log = LogManager.getLogger();	
	
	public static class Commiter {
		public static final int maxTileWriteCount = 256;
		private final RasterUnit targetUnit;
		private int tileWriteCount = 0;
		private long totalWriteCount = 0;
		
		public Commiter(RasterUnit targetUnit) {
			this.targetUnit = targetUnit;
		}
		
		public void add(int tilesWrittenInBand) {
			tileWriteCount += tilesWrittenInBand;
			checkCommit();
		}
		
		private void checkCommit() {
			if(maxTileWriteCount <= tileWriteCount) {
				log.info("commit");
				targetUnit.commit();
				totalWriteCount += tileWriteCount;
				tileWriteCount = 0;
			}
		}
		
		public void checkFinishCommit() {
			if(0 < tileWriteCount) {
				log.info("commit");
				targetUnit.commit();
				totalWriteCount += tileWriteCount;
				tileWriteCount = 0;
			}
		}

		public long getTotalWriteCount() {
			return totalWriteCount;
		}
	}
	
	public static long writeRasterUnitDiv4(RasterDB rasterdb, RasterUnit sourceUnit, RasterUnit targetUnit) {
		Commiter commiter = new Commiter(targetUnit);
		for(BandKey bandKey:sourceUnit.bandKeysReadonly) {
			Band band = rasterdb.bandMap.get(bandKey.b);
			switch (band.type) {
			case TilePixel.TYPE_SHORT:
				ProcessingShort.writeRasterUnitBandDiv4(sourceUnit, targetUnit, bandKey, band, commiter);				
				break;
			case TilePixel.TYPE_FLOAT:
				ProcessingFloat.writeRasterUnitBandDiv4(sourceUnit, targetUnit, bandKey, band, commiter);				
				break;				
			default:
				throw new RuntimeException("unknown band type");
			}

		}		
		commiter.checkFinishCommit();
		return commiter.getTotalWriteCount();
	}
}
