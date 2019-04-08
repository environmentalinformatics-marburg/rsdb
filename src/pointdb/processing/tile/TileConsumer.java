package pointdb.processing.tile;

import pointdb.base.Tile;

public interface TileConsumer {
	void nextTile(Tile tile);
}
