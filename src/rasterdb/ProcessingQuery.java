package rasterdb;

import java.util.Collection;

import rasterunit.RasterUnit;
import rasterunit.Tile;
import util.Range2d;

public class ProcessingQuery {
	//private static final Logger log = LogManager.getLogger();

	public static boolean mayHavePixels(RasterUnit rasterUnit, int t, Band band, Range2d pixelRange) {
		return mayHavePixels(rasterUnit, t, band, pixelRange.ymin, pixelRange.ymax, pixelRange.xmin, pixelRange.xmax);
	}

	public static boolean mayHavePixels(RasterUnit rasterUnit, int t, Band band, int pymin, int pymax, int pxmin, int pxmax) {
		int ymin = TilePixel.pixelToTile(pymin);
		int ymax = TilePixel.pixelToTile(pymax);
		int xmin = TilePixel.pixelToTile(pxmin); 
		int xmax = TilePixel.pixelToTile(pxmax);
		Collection<Tile> tiles = rasterUnit.getTiles(t, band.index, ymin, ymax, xmin, xmax);
		return !tiles.isEmpty();
	}
}
