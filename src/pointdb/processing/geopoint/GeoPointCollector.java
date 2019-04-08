package pointdb.processing.geopoint;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

public class GeoPointCollector {
	
	public static class Processor implements GeoPointConsumer {
		public final Vec<GeoPoint> result;
		
		public Processor() {
			this.result = new Vec<GeoPoint>(1000);
		}

		@Override
		public void nextGeoPoint(GeoPoint geoPoint) {
			result.add(geoPoint);			
		}		
	}
	
	public static Vec<GeoPoint> toList(GeoPointProducer geoPointProducer) {
		Processor processing = new Processor();
		geoPointProducer.produce(processing);
		return processing.result;
	}

}
