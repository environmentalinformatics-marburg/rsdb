package pointdb;

import pointdb.base.TileKey;
import pointdb.processing.tilekey.TileKeyConsumer;
import pointdb.processing.tilekey.TileKeyProducer;

public class DBFullTileKeyProducer implements TileKeyProducer{
	private final PointDB pointdb;
	
	public static DBFullTileKeyProducer of(PointDB pointdb) {
		return new DBFullTileKeyProducer(pointdb);
	}
	
	public DBFullTileKeyProducer(PointDB pointdb) {
		this.pointdb = pointdb;
	}
	
	@Override
	public void produce(TileKeyConsumer tileKeyConsumer) {
		for(TileKey tileKey:pointdb.tileMetaMap.keySet()) {
			tileKeyConsumer.nextTileKey(tileKey);
		}
	}
}
