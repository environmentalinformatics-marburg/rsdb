package pointdb.processing.geopoint;

import pointdb.base.GeoPoint;

public class GeoPointHeightProcessor implements GeoPointProducer {
	
	private final GeoPointProducer geoPointProducer;
	private final RasterSubGrid dtm;

	public GeoPointHeightProcessor(GeoPointProducer geoPointProducer, RasterSubGrid dtm) {
		this.geoPointProducer = geoPointProducer;
		this.dtm = dtm;
	}
	
	private static class Processor implements GeoPointConsumer{
		
		private final GeoPointConsumer geoPointConsumer;
		private final int lx;
		private final int ly;
		private final double[][] data;

		public Processor(GeoPointConsumer geoPointConsumer, RasterSubGrid dtm) {
			System.out.println(dtm.local_min_x+"   " +dtm.start_x);
			this.geoPointConsumer = geoPointConsumer;
			this.lx = dtm.local_min_x - dtm.start_x;
			this.ly = dtm.local_min_y - dtm.start_y;
			this.data = dtm.data;
		}

		@Override
		public void nextGeoPoint(GeoPoint p) {			
			int mx = ((int)p.x) - lx;
			int my = ((int)p.y) - ly;
			double z = p.z - data[my][mx];
			GeoPoint pz = GeoPoint.of(p.x, p.y, z, p);
			geoPointConsumer.nextGeoPoint(pz);
		}		
	}

	@Override
	public void produce(GeoPointConsumer geoPointConsumer) {
		geoPointProducer.produce(new Processor(geoPointConsumer, dtm));		
	}

}
