package task.rasterdb;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.TileFloat;
import rasterdb.TilePixel;
import rasterdb.TileShort;
import rasterunit.BandKey;
import rasterunit.RasterUnit;
import rasterunit.Tile;
import rasterunit.TileKey;

public class Task_count_pixels {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_count_pixels(Broker broker, JSONObject task) {
		this.broker = broker;
		this.task = task;
	}

	public void run() {
		try {
			String name = task.getString("rasterdb");
			RasterDB rasterdb =  broker.getRasterdb(name);
			RasterUnit rasterunit = rasterdb.rasterUnit();
			BandKey bandKey = rasterunit.bandKeysReadonly.first();

			Band band = rasterdb.bandMap.get(bandKey.b);

			TileKey keyXmin = bandKey.toTileKeyMin();
			TileKey keyXmax = bandKey.toTileKeyMax();
			Collection<Tile> tiles = rasterunit.getTiles(keyXmin, keyXmax);
			long cnt = 0;
			switch(band.type) {
			case TilePixel.TYPE_SHORT: {
				int[] raw = new int[TilePixel.PIXELS_PER_TILE];
				for(Tile tile:tiles) {
					TileShort.decode_raw(tile.data, raw);  
					cnt += TileShort.countNotNa_raw(raw, 0);
				}
				break;
			}
			case TilePixel.TYPE_FLOAT: {
				int[] raw = new int[TilePixel.PIXELS_PER_TILE];
				float[] dst = new float[TilePixel.PIXELS_PER_TILE];
				for(Tile tile:tiles) {
					TileShort.decode_raw(tile.data, raw);  
					TileFloat.decode_raw(raw, dst);
					cnt += TileFloat.countNotNa_raw(dst);
				}
				break;
			}
			default:
				throw new RuntimeException("not implemented for band type " + band.type);
			}

			log.info("count_pixels " + cnt);
		} catch(Exception e) {
			log.error(e);
		}

	}
}
