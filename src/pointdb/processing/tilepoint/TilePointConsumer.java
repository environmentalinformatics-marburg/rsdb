package pointdb.processing.tilepoint;

import pointdb.base.Point;
import pointdb.base.Tile;

public interface TilePointConsumer {
	void nextTile(Tile tile);
	void nextPoint(Point point);
}