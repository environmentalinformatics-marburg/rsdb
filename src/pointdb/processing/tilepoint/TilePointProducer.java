package pointdb.processing.tilepoint;

import org.tinylog.Logger;

public interface TilePointProducer {
	void produce(TilePointConsumer tilePointConsumer);
	
	/**
	 * 
	 * @param filter 
	 * @return if filter==null => return this
	 */
	default TilePointProducer filter(PointFilter filter) {
		if(filter==null) {
			return this;
		}
		return TilePointFilterProcessor.of(this, filter);
	}
	
	default TilePointProducerInjected inject(TilePointConsumer injectedConsumer) {
		return new TilePointProducerInjected(this, injectedConsumer);
	}
	
	default GeoPointProcessor toGeoPointProducer() {
		return new GeoPointProcessor(this);
	}
	
	default void requestStop() {
		Logger.info("requestStop not implemented");
	}	
}
