package rasterdb.tile;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellInt16;
import rasterdb.cell.CellType;
import rasterdb.tile.Processing.Commiter;
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
}
