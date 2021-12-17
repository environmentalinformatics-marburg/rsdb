package server.api.pointdb.feature;


import org.tinylog.Logger;
import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.GeoPointConsumer;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tilepoint.PointFilter;

public class Feature_coverage extends Feature {
	

	private static class Consumer implements GeoPointConsumer {
		private int min_x;
		private int min_y;

		public int[][] counter;
		public Consumer(Rect rect) {
			Logger.info(rect);
			this.min_x = rect.getInteger_UTM_min_x();
			this.min_y = rect.getInteger_UTM_min_y();
			int max_x = rect.getInteger_UTM_max_x();
			int max_y = rect.getInteger_UTM_max_y();
			this.counter = new int[max_y-min_y+1][max_x-min_x+1];
		}
		@Override
		public void nextGeoPoint(GeoPoint geoPoint) {
			int x = (int) geoPoint.x;
			int y = (int) geoPoint.y;
			//			Logger.info(x+"   "+y);
			//			Logger.info((x-min_x)+"   "+(y-min_y));
			//			Logger.info(min_x+"   "+min_y);
			counter[y-min_y][x-min_x]++;
		}

		public double getCoverage() {
			int c = 0;
			for (int y = 0; y < counter.length; y++) {
				int[] row = counter[y];
				for (int x = 0; x < counter.length; x++) {
					if(row[x]>0) {
						c++;
					}
				}
			}
			return ((double) c)/(counter.length*counter[0].length);
		}
	}

	@Override
	public void calc(JSONWriter json, PointDB db, Rect rect) {

		Logger.info(rect);

		Rect outerRect = rect.outerMeterRect();

		Consumer consumer = new Consumer(outerRect);
		TileConsumer injectedConsumer = tile->Logger.info(tile);

		db.tileProducer(outerRect)
		.inject(injectedConsumer)
		.toTilePointProducer(outerRect)
		.filter(PointFilter.createAtomicFilter("last_return=1"))
		.toGeoPointProducer()
		.produce(consumer);



		db.tilePointProducer(outerRect).filter(PointFilter.createAtomicFilter("last_return=1")).toGeoPointProducer().produce(consumer);
		json.object();
		json.key("coverage");
		json.value(consumer.getCoverage());
		json.endObject();		
	}

}
