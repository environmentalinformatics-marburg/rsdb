package pointdb.processing.tilekey;

import pointdb.base.TileKey;

public interface TileKeyConsumer {
	void nextTileKey(TileKey tileKey);
}
