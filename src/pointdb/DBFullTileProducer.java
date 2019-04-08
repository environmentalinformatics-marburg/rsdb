package pointdb;

import pointdb.base.Tile;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;

public class DBFullTileProducer implements TileProducer {
	private final PointDB pointdb;
	private volatile boolean requestedStop = false;
	
	public static TileProducer of(PointDB pointdb) {
		return new DBFullTileProducer(pointdb);
	}
	
	public DBFullTileProducer(PointDB pointdb) {
		this.pointdb = pointdb;
	}
	
	@Override
	public void produce(TileConsumer consumer) {
		for(Tile tile:pointdb.tileMap.values()) {
			consumer.nextTile(tile);
			if(requestedStop) {
				break;
			}
		}		
	}

	@Override
	public void requestStop() {
		requestedStop = true;
	}	
}
