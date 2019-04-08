package pointdb;

import pointdb.base.TileMeta;
import pointdb.processing.tilemeta.TileMetaConsumer;
import pointdb.processing.tilemeta.TileMetaProducer;

public class DBFullTileMetaProducer implements TileMetaProducer {
	private final PointDB pointdb;
	
	public static TileMetaProducer of(PointDB pointdb) {
		return new DBFullTileMetaProducer(pointdb);
	}
	
	public DBFullTileMetaProducer(PointDB pointdb) {
		this.pointdb = pointdb;
	}
	
	@Override
	public void produce(TileMetaConsumer tileMetaConsumer) {
		for(TileMeta tileMeta:pointdb.tileMetaMap.values()) {
			tileMetaConsumer.nextTileMeta(tileMeta);
		}
	}
}
