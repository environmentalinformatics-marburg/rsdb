package rasterdb.tile;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellInt16;
import rasterdb.cell.CellType;
import rasterunit.BandKey;
import rasterunit.RasterUnitStorage;

public class Processing {
	private static final Logger log = LogManager.getLogger();	
	
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
	
	public static long writeRasterUnitDiv4(RasterDB rasterdb, RasterUnitStorage rasterUnitStorage, RasterUnitStorage targetUnit) throws IOException {
		Commiter commiter = new Commiter(targetUnit);
		for(BandKey bandKey:rasterUnitStorage.bandKeysReadonly()) {
			Band band = rasterdb.bandMap.get(bandKey.b);
			switch (band.type) {
			case TilePixel.TYPE_SHORT:
				ProcessingShort.writeRasterUnitBandDiv4(rasterUnitStorage, targetUnit, bandKey, band, commiter);				
				break;
			case TilePixel.TYPE_FLOAT:
				ProcessingFloat.writeRasterUnitBandDiv4(rasterUnitStorage, targetUnit, bandKey, band, commiter);				
				break;
			case CellType.INT16:
				CellInt16 cellInt16 = new CellInt16(targetUnit.getTilePixelLen());
				cellInt16.writeRasterUnitBandDiv4(rasterUnitStorage, targetUnit, bandKey, band, commiter);		
				break;
			default:
				throw new RuntimeException("unknown band type");
			}

		}		
		commiter.checkFinishCommit();
		return commiter.getTotalWriteCount();
	}
}
