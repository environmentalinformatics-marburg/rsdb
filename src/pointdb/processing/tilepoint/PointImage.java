package pointdb.processing.tilepoint;

import util.image.ImageRGBA;

public abstract class PointImage extends ImageRGBA implements TilePointConsumer {

	public PointImage(int width, int height) {
		super(width, height);
	}
}
