package pointdb.processing.tilepoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.base.Rect;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import util.image.ImageRGBA;

public class ImageCreator {
	private static final Logger log = LogManager.getLogger();

	private final TilePointProducer tilePointProducer;
	private final Statistics tileMinMaxCalc;
	private final long utmm_min_x;
	private final long utmm_min_y;
	private final int screen_width;
	private final int screen_height;
	private final int TILE_LOCAL_TO_SCREEN_DIV;
	private final boolean fill;
	private final ImageType imageType;
	private volatile boolean requestedStop = false;

	public static ImageCreator of(TilePointProducer tilePointProducer, Statistics tileMinMaxCalc, Rect rect, int screen_width, int screen_height, int TILE_LOCAL_TO_SCREEN_DIV, boolean fill, ImageType imageType) {
		return new ImageCreator(tilePointProducer, tileMinMaxCalc, rect, screen_width, screen_height, TILE_LOCAL_TO_SCREEN_DIV, fill, imageType);
	}

	private ImageCreator(TilePointProducer tilePointProducer, Statistics tileMinMaxCalc, Rect rect, int screen_width, int screen_height, int TILE_LOCAL_TO_SCREEN_DIV, boolean fill, ImageType imageType)  {
		this.tilePointProducer = tilePointProducer;
		this.tileMinMaxCalc = tileMinMaxCalc;
		this.utmm_min_x = rect.utmm_min_x;
		this.utmm_min_y = rect.utmm_min_y;
		this.screen_width = screen_width;
		this.screen_height = screen_height;
		this.TILE_LOCAL_TO_SCREEN_DIV = TILE_LOCAL_TO_SCREEN_DIV;
		this.fill = fill;
		this.imageType = imageType;
	}

	public ImageRGBA create() {
		PointImage pointImage;

		switch(imageType) {
		case INTENSITY:
			pointImage = new PointImageIntensity(utmm_min_x, utmm_min_y, screen_width, screen_height, TILE_LOCAL_TO_SCREEN_DIV, imageType, tileMinMaxCalc);
			break;
		case Z:
			pointImage = new PointImageZ(utmm_min_x, utmm_min_y, screen_width, screen_height, TILE_LOCAL_TO_SCREEN_DIV, imageType, tileMinMaxCalc);
			break;
		case INTENSITY_Z:
			pointImage = new PointImageIntensityZ(utmm_min_x, utmm_min_y, screen_width, screen_height, TILE_LOCAL_TO_SCREEN_DIV, imageType, tileMinMaxCalc);
			break;
		default:
			throw new RuntimeException("unknown image type "+imageType);
		}

		if(requestedStop) {
			return null;
		}
		tilePointProducer.produce(pointImage);
		if(requestedStop) {
			return null;
		}
		if(fill) {
			pointImage.fillPixel();
		}
		return pointImage;
	}

	public void softCancel() {
		log.info("softCancel");
		tilePointProducer.requestStop();
		requestedStop = true;
	}
}
