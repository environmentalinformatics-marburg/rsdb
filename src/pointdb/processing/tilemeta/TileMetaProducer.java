package pointdb.processing.tilemeta;

import pointdb.processing.tilemeta.StatisticsCreator.Statistics;

public interface TileMetaProducer {
	void produce(TileMetaConsumer tileMetaConsumer);
	
	default Statistics toStatistics() {
		return StatisticsCreator.direct(this);
	}
	
	default TileMetaAggregator aggregate(int aggregation_tile_size) {
		return new TileMetaAggregator(this, aggregation_tile_size);
	}
}
