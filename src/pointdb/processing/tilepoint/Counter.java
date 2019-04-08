package pointdb.processing.tilepoint;

import pointdb.base.Point;
import pointdb.base.Tile;

public class Counter {
	
	private final TilePointProducer tilePointProducer;
	
	public Counter(TilePointProducer tilePointProducer){
		this.tilePointProducer = tilePointProducer;
	}
	
	private static class Processor implements TilePointConsumer {		
		public long counter=0;
		@Override
		public void nextTile(Tile tile) {}
		@Override
		public void nextPoint(Point point) {
			counter++;
		}		
	}
	
	public long count() {
		Processor processor = new Processor();
		tilePointProducer.produce(processor);
		return processor.counter;
	}
}
