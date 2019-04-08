package pointdb.processing.tilepoint;

import pointdb.base.GeoPoint;
import pointdb.base.PdbConst;
import pointdb.base.Point;
import pointdb.base.Tile;
import pointdb.processing.geopoint.GeoPointConsumer;
import pointdb.processing.geopoint.GeoPointProducer;

public class GeoPointProcessor implements GeoPointProducer {
	
	private final TilePointProducer tilePointProducer;
	
	public GeoPointProcessor(TilePointProducer tilePointProducer) {
		this.tilePointProducer = tilePointProducer;
	}
	
	public static class Processor implements TilePointConsumer {
		private static final double LOCAL_SCALE = PdbConst.LOCAL_SCALE_FACTOR;
		
		private final GeoPointConsumer geoPointConsumer;

		private double tile_x;
		private double tile_y;
		
		public Processor(GeoPointConsumer geoPointConsumer) {
			this.geoPointConsumer = geoPointConsumer;
		}

		@Override
		public void nextTile(Tile tile) {
			this.tile_x = tile.meta.x;
			this.tile_y = tile.meta.y;			
		}

		@Override
		public void nextPoint(Point point) {
			double x = tile_x+point.x/LOCAL_SCALE;
			double y = tile_y+point.y/LOCAL_SCALE;
			double z = point.z/LOCAL_SCALE;
			GeoPoint geoPoint = GeoPoint.of(x, y, z, point);
			geoPointConsumer.nextGeoPoint(geoPoint);			
		}
		
	}
	
	@Override
	public void produce(GeoPointConsumer geoPointConsumer) {
		tilePointProducer.produce(new Processor(geoPointConsumer));
	}
}
