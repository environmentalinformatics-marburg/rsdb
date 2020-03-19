package remotetask.rasterdb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.RasterdbConfig;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileKey;

public class Rebuild {
	private static final Logger log = LogManager.getLogger();

	private final RasterDB src;
	private final Path rootPath;
	private final String storage_type;

	public Rebuild(RasterDB src, Path rootPath, String storage_type) {
		this.src = src;
		this.rootPath = rootPath;
		this.storage_type = storage_type;
	}

	public void run() throws IOException {
		RasterUnitStorage srcStorage = src.rasterUnit();
		String name_src = src.config.getName();
		String name_dst = name_src + "_rebuild";
		Path path_dst = rootPath.resolve(name_dst);
		RasterdbConfig dstConfig = RasterdbConfig.ofPath(path_dst, storage_type);
		dstConfig.set_fast_unsafe_import(true);
		try(RasterDB dst = new RasterDB(dstConfig)) {
			dst.setACL(src.getACL());
			dst.setACL_mod(src.getACL_mod());
			GeoReference srcRef = src.ref();
			dst.setCode(srcRef.code);
			dst.setProj4(srcRef.proj4);
			dst.setInformal(src.informal());
			dst.setPixelSize(srcRef.pixel_size_x, srcRef.pixel_size_y, srcRef.offset_x, srcRef.offset_y);
			RasterUnitStorage dstStorage = dst.rasterUnit();

			Iterator<TileKey> it = srcStorage.tileKeysReadonly().iterator();

			int batchCnt = 0;
			int batchMax = 256;
			Tile[] batchCollector = new Tile[batchMax];
			while(it.hasNext()) {
				TileKey tileKey = it.next();
				Tile tile = srcStorage.readTile(tileKey);
				batchCollector[batchCnt++] = tile;
				if(batchCnt == batchMax) {
					write(dstStorage, batchCollector, batchCnt);
					batchCnt = 0;
				}
			}
			if(batchCnt > 0) {
				write(dstStorage, batchCollector, batchCnt);
				batchCnt = 0;
			}
			dstStorage.flush();
			for(Band band:src.bandMap.values()) {
				dst.setBand(band);
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
