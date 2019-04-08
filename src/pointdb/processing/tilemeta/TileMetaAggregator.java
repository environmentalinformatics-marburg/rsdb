package pointdb.processing.tilemeta;

import java.util.HashMap;
import java.util.Map;

import pointdb.base.TileKey;
import pointdb.base.TileMeta;

public class TileMetaAggregator implements TileMetaProducer{
	
	private final TileMetaProducer tileMetaProducer;
	private final int aggregation_tile_size;
	
	public TileMetaAggregator(TileMetaProducer tileMetaProducer, int aggregation_tile_size) {
		this.tileMetaProducer = tileMetaProducer;
		this.aggregation_tile_size = aggregation_tile_size;
	}
	
	public static class Aggregator implements TileMetaProducer {
		protected Map<TileKey,TileMeta> aggMap = new HashMap<TileKey,TileMeta>();
		
		@Override
		public void produce(TileMetaConsumer tileMetaConsumer) {
			for(TileMeta meta:aggMap.values()) {
				tileMetaConsumer.nextTileMeta(meta);
			}
		}		
	}	
	
	public static class AggregatorProcessor extends Aggregator implements TileMetaConsumer {		
		private final int aggregation_tile_size;	
		
		public AggregatorProcessor(int aggregation_tile_size) {
			this.aggregation_tile_size = aggregation_tile_size;
		}

		@Override
		public void nextTileMeta(TileMeta meta) {
			int x = (meta.x/aggregation_tile_size)*aggregation_tile_size;
			int y = (meta.y/aggregation_tile_size)*aggregation_tile_size;
			TileKey key = new TileKey(x, y);
			TileMeta currMeta = new TileMeta(x, y, meta.point_count, meta.min, meta.max, meta.avg);
			TileMeta oldMeta = aggMap.get(key);
			if(oldMeta==null) {
				aggMap.put(key,currMeta);
			} else {
				TileMeta mergedMeta = TileMeta.merge(oldMeta,currMeta);
				aggMap.put(key,mergedMeta);
			}			
		}			
	}
	
	/**
	 * Processes all tileMetas from producer and creates new producer
	 * @return
	 */
	public Aggregator produceAggregator() {
		AggregatorProcessor aggregator = new AggregatorProcessor(aggregation_tile_size);
		tileMetaProducer.produce(aggregator);
		return aggregator;
	}

	@Override
	public void produce(TileMetaConsumer tileMetaConsumer) {
		produceAggregator().produce(tileMetaConsumer);
	}	
}
