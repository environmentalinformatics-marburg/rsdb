package pointdb.processing.tile;

import pointdb.base.Tile;

@FunctionalInterface
public interface TileConsumer {
	void nextTile(Tile tile);
}
