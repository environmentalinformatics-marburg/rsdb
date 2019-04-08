package pointdb.processing.tile;

import pointdb.base.PdbConst;
import pointdb.base.Point;
import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.processing.tilepoint.TilePointConsumer;
import pointdb.processing.tilepoint.TilePointProducer;

public class TilePointProcessor implements TilePointProducer{
	private final TileProducer tileProducer;
	private final long utmm_min_x;
	private final long utmm_min_y;
	private final long utmm_max_x;
	private final long utmm_max_y;
	
	public static TilePointProcessor of(TileProducer tileProducer, Rect rect) {
		return new TilePointProcessor(tileProducer, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}
	
	public static TilePointProcessor of_UTMM(TileProducer tileProducer, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		return new TilePointProcessor(tileProducer, utmm_min_x, utmm_min_y, utmm_max_x, utmm_max_y);
	}

	private TilePointProcessor(TileProducer tileProducer, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		this.tileProducer = tileProducer;
		this.utmm_min_x = utmm_min_x;
		this.utmm_min_y = utmm_min_y;
		this.utmm_max_x = utmm_max_x;
		this.utmm_max_y = utmm_max_y;
	}

	public static class Processor implements TileConsumer {
		private final TilePointConsumer tilePointConsumer;
		private final long utmm_min_x;
		private final long utmm_min_y;
		private final long utmm_max_x;
		private final long utmm_max_y;		
		private final int tile_utm_min_x;
		private final int tile_utm_min_y;
		private final int tile_utm_max_x;
		private final int tile_utm_max_y;
		
		public Processor(TilePointConsumer tilePointConsumer, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
			this.tilePointConsumer = tilePointConsumer;
			this.utmm_min_x = utmm_min_x;
			this.utmm_min_y = utmm_min_y;
			this.utmm_max_x = utmm_max_x;
			this.utmm_max_y = utmm_max_y;
			int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
			int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
			int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
			int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);		
			this.tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
			this.tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
			this.tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
			this.tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);			
		}
		
		@Override
		public void nextTile(Tile tile) {
			tilePointConsumer.nextTile(tile);
			int tile_utm_x = tile.meta.x;
			int tile_utm_y = tile.meta.y;
			long tile_utmm_x = ((long)tile_utm_x)*PdbConst.LOCAL_SCALE_FACTOR;
			long tile_utmm_y = ((long)tile_utm_y)*PdbConst.LOCAL_SCALE_FACTOR;

			if(   tile_utm_x>tile_utm_min_x
					&& tile_utm_x<tile_utm_max_x
					&& tile_utm_y>tile_utm_min_y
					&& tile_utm_y<tile_utm_max_y) {
				Point[] points = tile.points;
				int n = points.length;
				for (int i = 0; i < n; i++) {
					tilePointConsumer.nextPoint(points[i]);
				}
			} else {
				Point[] points = tile.points;
				int n = points.length;
				for (int i = 0; i < n; i++) {
					Point point = points[i];
					long point_utmm_x = tile_utmm_x+point.x;
					long point_utmm_y = tile_utmm_y+point.y;
					if(   utmm_min_x<=point_utmm_x 
							&& point_utmm_x<=utmm_max_x 
							&& utmm_min_y<=point_utmm_y 
							&& point_utmm_y<=utmm_max_y) {
						tilePointConsumer.nextPoint(point);
					}
				}
			}		
		}
	}
	
	@Override
	public void produce(TilePointConsumer tilePointConsumer) {
		tileProducer.produce(new Processor(tilePointConsumer, utmm_min_x, utmm_min_y, utmm_max_x, utmm_max_y));
	}
	
	@Override
	public void requestStop() {
		tileProducer.requestStop();
	}
}
