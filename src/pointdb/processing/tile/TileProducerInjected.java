package pointdb.processing.tile;

import pointdb.base.Tile;

public class TileProducerInjected implements TileProducer{
	private TileProducer producer;
	private TileConsumer injectedConsumer;
	
	public TileProducerInjected(TileProducer producer, TileConsumer injectedConsumer) {
		this.producer = producer;
		this.injectedConsumer = injectedConsumer;
	}
	
	public static class Processor implements TileConsumer{
		private TileConsumer injectedConsumer;
		private TileConsumer tileConsumer;
		public Processor(TileConsumer injectedConsumer, TileConsumer tileConsumer) {
			this.injectedConsumer = injectedConsumer;
			this.tileConsumer = tileConsumer;					
		}
		@Override
		public void nextTile(Tile tile) {
			injectedConsumer.nextTile(tile);
			tileConsumer.nextTile(tile);
		}
		
	}
	
	@Override
	public void produce(TileConsumer tileConsumer) {
		Processor processor = new Processor(injectedConsumer, tileConsumer);
		producer.produce(processor);			
	}
	@Override
	public void requestStop() {
		producer.requestStop();
	}		
}