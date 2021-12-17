package pointdb.processing.tile;



import org.tinylog.Logger;

import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.processing.tilepoint.TilePointProducer;
import util.collections.vec.Vec;

public interface TileProducer {
	void produce(TileConsumer tileConsumer);

	default TilePointProducer toTilePointProducer(Rect rect) {
		return TilePointProcessor.of(this, rect);
	}

	default TilePointProducer toFullTilePointProducer() {
		return FullTilePointProcessor.of(this);
	}
	
	default Vec<Tile> toVec() {
		return TileVecCollector.toVec(this);
	}

	default TileProducerInjected inject(TileConsumer injectedConsumer) {
		return new TileProducerInjected(this, injectedConsumer);
	}

	default void requestStop() {
		Logger.info("requestStop not implemented");
	}
}
