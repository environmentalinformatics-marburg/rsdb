package pointdb.processing.tile;

import pointdb.base.Point;
import pointdb.base.Tile;
import pointdb.processing.tilepoint.TilePointConsumer;
import pointdb.processing.tilepoint.TilePointProducer;

public class FullTilePointProcessor implements TilePointProducer {
	private final TileProducer tileProducer;
	
	public static FullTilePointProcessor of(TileProducer tileProducer) {
		return new FullTilePointProcessor(tileProducer);
	}

	public FullTilePointProcessor(TileProducer tileProducer) {
		this.tileProducer = tileProducer;
	}

	public static class Processor implements TileConsumer {
		private final TilePointConsumer tilePointConsumer;
		public Processor(TilePointConsumer tilePointConsumer) {
			this.tilePointConsumer = tilePointConsumer;
		}
		@Override
		public void nextTile(Tile tile) {
			tilePointConsumer.nextTile(tile);
			for(Point point:tile.points) {
				tilePointConsumer.nextPoint(point);
			}		
		}
	}
	
	@Override
	public void produce(TilePointConsumer tilePointConsumer) {
		tileProducer.produce(new Processor(tilePointConsumer));
	}
	
	@Override
	public void requestStop() {
		tileProducer.requestStop();
	}
}
