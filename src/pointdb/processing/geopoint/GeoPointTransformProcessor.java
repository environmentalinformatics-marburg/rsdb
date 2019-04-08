package pointdb.processing.geopoint;

import pointdb.base.GeoPoint;

public class GeoPointTransformProcessor implements GeoPointProducer {
	
	private final GeoPointProducer geoPointProducer;
	private final double tx;
	private final double ty;

	public GeoPointTransformProcessor(GeoPointProducer geoPointProducer, double tx, double ty) {
		this.geoPointProducer = geoPointProducer;
		this.tx = tx;
		this.ty = ty;
	}
	
	private static class Processor implements GeoPointConsumer{
		
		private final GeoPointConsumer geoPointConsumer;
		private final double tx;
		private final double ty;

		public Processor(GeoPointConsumer geoPointConsumer, double tx, double ty) {
			this.geoPointConsumer = geoPointConsumer;
			this.tx = tx;
			this.ty = ty;
		}

		@Override
		public void nextGeoPoint(GeoPoint geoPoint) {
			geoPointConsumer.nextGeoPoint(GeoPoint.of(geoPoint.x+tx, geoPoint.y+ty, geoPoint.z, geoPoint));			
		}		
	}

	@Override
	public void produce(GeoPointConsumer geoPointConsumer) {
		geoPointProducer.produce(new Processor(geoPointConsumer, tx, ty));		
	}

}
