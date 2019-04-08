package pointdb.processing.geopoint;

import pointdb.base.GeoPoint;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;

public class PolygonClippingProcessor implements GeoPointProducer {
	
	private final GeoPointProducer geoPointProducer;
	private final Point2d[] polygon;

	public PolygonClippingProcessor(GeoPointProducer geoPointProducer, Point2d[] polygon) {
		this.geoPointProducer = geoPointProducer;
		this.polygon = polygon;
	}
	
	private static class Processor implements GeoPointConsumer {
		
		private final GeoPointConsumer geoPointConsumer;
		private final Point2d[] polygon;
		
		public Processor(GeoPointConsumer geoPointConsumer, Point2d[] polygon) {
			this.geoPointConsumer = geoPointConsumer;
			this.polygon = polygon;
		}		

		@Override
		public void nextGeoPoint(GeoPoint geoPoint) {
			if(PolygonUtil.wn_PnPoly(geoPoint, polygon) != 0) {
				geoPointConsumer.nextGeoPoint(geoPoint);
			}			
		}		
	}

	@Override
	public void produce(GeoPointConsumer geoPointConsumer) {
		geoPointProducer.produce(new Processor(geoPointConsumer, polygon));
	}
}
