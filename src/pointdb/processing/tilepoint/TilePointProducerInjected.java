package pointdb.processing.tilepoint;

import pointdb.base.Point;
import pointdb.base.Tile;

public class TilePointProducerInjected implements TilePointProducer  {
	private TilePointProducer producer;
	private TilePointConsumer injectedConsumer;
	
	public TilePointProducerInjected(TilePointProducer producer, TilePointConsumer injectedConsumer) {
		this.producer = producer;
		this.injectedConsumer = injectedConsumer;
	}
	
	public static class Processor implements TilePointConsumer {
		private TilePointConsumer injectedConsumer;
		private TilePointConsumer consumer;
		public Processor(TilePointConsumer injectedConsumer, TilePointConsumer consumer) {
			this.injectedConsumer = injectedConsumer;
			this.consumer = consumer;					
		}
		@Override
		public void nextTile(Tile tile) {
			injectedConsumer.nextTile(tile);
			consumer.nextTile(tile);
		}
		@Override
		public void nextPoint(Point point) {
			injectedConsumer.nextPoint(point);
			consumer.nextPoint(point);			
		}		
	}
	
	@Override
	public void produce(TilePointConsumer consumer) {
		Processor processor = new Processor(injectedConsumer, consumer);
		producer.produce(processor);			
	}
	@Override
	public void requestStop() {
		producer.requestStop();
	}
}
