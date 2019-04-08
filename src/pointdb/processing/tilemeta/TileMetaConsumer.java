package pointdb.processing.tilemeta;

import pointdb.base.TileMeta;

public interface TileMetaConsumer {
	void nextTileMeta(TileMeta tileMeta);
}
