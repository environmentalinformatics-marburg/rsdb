package pointdb.processing.tilekey;

import pointdb.base.TileKey;

public class TileKeyIsEmptyCollector {
	
	public static class Processor implements TileKeyConsumer {
		public boolean isEmpty = true;
		@Override
		public void nextTileKey(TileKey tileKey) {
			isEmpty = false;			
		}
		
	}
	
	public static boolean isEmpty(TileKeyProducer producer) {
		Processor processor = new Processor();
		producer.produce(processor);
		return processor.isEmpty;
	}

}
