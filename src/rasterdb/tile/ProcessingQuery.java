package rasterdb.tile;

import java.util.Collection;

import rasterdb.Band;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import util.Range2d;

public class ProcessingQuery {
	//

	public static boolean mayHavePixels(RasterUnitStorage pyramid_rasterUnit, int t, Band band, Range2d pixelRange) {
		return mayHavePixels(pyramid_rasterUnit, t, band, pixelRange.ymin, pixelRange.ymax, pixelRange.xmin, pixelRange.xmax);
	}

	public static boolean mayHavePixels(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int pymin, int pymax, int pxmin, int pxmax) {
		int ymin = TilePixel.pixelToTile(pymin);
		int ymax = TilePixel.pixelToTile(pymax);
		int xmin = TilePixel.pixelToTile(pxmin); 
		int xmax = TilePixel.pixelToTile(pxmax);
		Collection<Tile> tiles = pyramid_rasterUnit.readTiles(t, band.index, ymin, ymax, xmin, xmax);
		return !tiles.isEmpty();
	}
}
