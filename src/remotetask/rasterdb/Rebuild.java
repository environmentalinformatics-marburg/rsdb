package remotetask.rasterdb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.RasterdbConfig;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileKey;
import remotetask.CancelableRemoteProxy;

public class Rebuild extends CancelableRemoteProxy {
	private static final Logger log = LogManager.getLogger();

	private final RasterDB src;
	private final Path rootPath;
	private final String storage_type;
	private final String pyramid_type; // nullable

	public Rebuild(RasterDB src, Path rootPath, String storage_type, String pyramid_type) {
		this.src = src;
		this.rootPath = rootPath;
		this.storage_type = storage_type;
		this.pyramid_type = pyramid_type;
	}

	@Override
	public void process() throws IOException {
		RasterUnitStorage srcStorage = src.rasterUnit();
		String name_src = src.config.getName();
		String name_dst = name_src + "_rebuild";
		Path path_dst = rootPath.resolve(name_dst);
		RasterdbConfig dstConfig = RasterdbConfig.ofPath(path_dst, storage_type);
		dstConfig.preferredPyramidType = pyramid_type;
		dstConfig.set_fast_unsafe_import(true);
		dstConfig.preferredTilePixelLen = src.getTilePixelLen();
		try(RasterDB dst = new RasterDB(dstConfig)) {
			dst.setACL(src.getACL());
			dst.setACL_mod(src.getACL_mod());
			dst.setRef(src.ref());
			dst.setInformal(src.informal());
			dst.setAssociated(src.associated.copy());
			RasterUnitStorage dstStorage = dst.rasterUnit();

			int totalTiles = srcStorage.tileKeysReadonly().size();
			Iterator<TileKey> it = srcStorage.tileKeysReadonly().iterator();

			long totalWritten = 0;
			int batchCnt = 0;
			int batchMax = 256;
			Tile[] batchCollector = new Tile[batchMax];
			while(!isCanceled() && it.hasNext()) {
				TileKey tileKey = it.next();
				Tile tile = srcStorage.readTile(tileKey);
				batchCollector[batchCnt++] = tile;
				if(batchCnt == batchMax) {
					write(dstStorage, batchCollector, batchCnt);
					totalWritten += batchCnt;
					batchCnt = 0;
					setMessage("tiles written: " + totalWritten + " of " + totalTiles + "   " + (( totalWritten * 100) / totalTiles) + "%");
				}
			}
			if(batchCnt > 0) {
				write(dstStorage, batchCollector, batchCnt);
				batchCnt = 0;
			}
			dstStorage.flush();
			for(Band band:src.bandMapReadonly.values()) {
				dst.setBand(band, true);
			}
			dst.setTimeSlices(src.timeMapReadonly.values());
			dst.setCustomWMS(src.customWmsMapReadonly);
			
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}

			dst.rebuildPyramid(true);
		}
	}

	private void write(RasterUnitStorage dstStorage, Tile[] batchCollector, int batchCnt) throws IOException {
		log.info("write batch");
		for (int i = 0; i < batchCnt; i++) {
			dstStorage.writeTile(batchCollector[i]);
		}
		dstStorage.commit();
	}
}
