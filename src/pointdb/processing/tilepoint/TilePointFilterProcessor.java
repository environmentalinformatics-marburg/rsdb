package pointdb.processing.tilepoint;

import pointdb.base.Point;
import pointdb.base.Tile;

public class TilePointFilterProcessor implements TilePointProducer {
	private final TilePointProducer producer;
	private final PointFilter filter;
	
	public static TilePointProducer of(TilePointProducer producer, PointFilter filter) {
		if(filter==null) {
			return producer;
		}
		return new TilePointFilterProcessor(producer, filter);
	}
	
	private TilePointFilterProcessor(TilePointProducer producer, PointFilter filter) {
		this.producer = producer;
		this.filter = filter;
	}
	
	public static class Processor implements TilePointConsumer {
		private final PointFilter filter;
		private final TilePointConsumer tilePointFilterConsumer;
		
		public Processor(PointFilter filter, TilePointConsumer tilePointFilterConsumer) {
			this.filter = filter;
			this.tilePointFilterConsumer = tilePointFilterConsumer;
		}

		@Override
		public void nextTile(Tile tile) {
			tilePointFilterConsumer.nextTile(tile);			
		}

		@Override
		public void nextPoint(Point point) {
			if(filter.test(point)) {
				tilePointFilterConsumer.nextPoint(point);
			}			
		}		
	}

	@Override
	public void produce(TilePointConsumer tilePointFilterConsumer) {
		producer.produce(new Processor(filter, tilePointFilterConsumer));		
	}
	
	@Override
	public void requestStop() {
		producer.requestStop();
	}
}