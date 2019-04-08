package pointdb.processing.tile;

import pointdb.base.Tile;
import util.collections.vec.Vec;

public class TileVecCollector {
	
	public static Vec<Tile> toVec(TileProducer tileProducer) {
		Vec<Tile> vec = new Vec<Tile>();
		tileProducer.produce(vec::add);
		return vec;
	}
}
