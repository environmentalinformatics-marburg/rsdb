package pointdb.processing.tilekey;

import pointdb.base.Rect;
import pointdb.base.TileKey;

public class StatisticsCollector implements TileKeyConsumer {
	
	public int tile_x_min = Integer.MAX_VALUE;
	public int tile_y_min = Integer.MAX_VALUE;
	public int tile_x_max = Integer.MIN_VALUE;
	public int tile_y_max = Integer.MIN_VALUE;
	public long tile_count = 0;
	
	private StatisticsCollector() {}
	
	public static StatisticsCollector collect(TileKeyProducer producer) {
		StatisticsCollector c = new StatisticsCollector();
		producer.produce(c);
		return c;
	}

	@Override
	public void nextTileKey(TileKey tileKey) {
		if(tileKey.x<tile_x_min) tile_x_min = tileKey.x;
		if(tileKey.y<tile_y_min) tile_y_min = tileKey.y;
		if(tile_x_max<tileKey.x) tile_x_max = tileKey.x; 
		if(tile_y_max<tileKey.y) tile_y_max = tileKey.y;
		tile_count++;		
	}
	
	public Rect toRect() {
		return Rect.of_UTM(tile_x_min, tile_y_min, tile_x_max, tile_y_max);
	}

}
