package remotetask.rasterdb;

import java.util.Collection;

import org.json.JSONObject;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellInt16;
import rasterdb.cell.CellType;
import rasterdb.cell.Int16;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterdb.tile.TileShort;
import rasterunit.BandKey;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileKey;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_rasterdb("count_pixels")
@Description("Count all pixels that are not NA. Just pixels of first band are counted.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer.", example="raster1")
public class Task_count_pixels extends RemoteTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB rasterdb;

	public Task_count_pixels(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		this.rasterdb =  broker.getRasterdb(name);
		rasterdb.check(ctx.userIdentity);
	}

	@Override
	protected void process() throws Exception {		
		RasterUnitStorage rasterunit = rasterdb.rasterUnit();
		BandKey bandKey = rasterunit.bandKeysReadonly().first();

		Band band = rasterdb.bandMapReadonly.get(bandKey.b);

		TileKey keyXmin = bandKey.toTileKeyMin();
		TileKey keyXmax = bandKey.toTileKeyMax();
		Collection<Tile> tiles = rasterunit.getTiles(keyXmin, keyXmax);
		setMessage("started");
		Thread.sleep(10000);
		long cnt = 0;
		switch(band.type) {
		case TilePixel.TYPE_SHORT: {
			int[] raw = new int[TilePixel.PIXELS_PER_TILE];
			for(Tile tile:tiles) {
				TileShort.decode_raw(tile.data, raw);  
				cnt += TileShort.countNotNa_raw(raw, band.getInt16NA());
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
		case CellType.INT16: {
			for(Tile tile:tiles) {
				CellInt16 cellInt16 = new CellInt16(rasterdb.getTilePixelLen());
				int[] raw = cellInt16.dec(tile.data);
				cnt += Int16.countNotNa_raw(raw, band.getInt16NA());
			}
			break;
		}
		default:
			throw new RuntimeException("not implemented for band type " + band.type);
		}

		setMessage("count_pixels " + cnt);
	}
}
