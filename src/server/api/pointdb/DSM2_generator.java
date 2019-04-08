package server.api.pointdb;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.RasterGrid;
import util.collections.vec.Vec;

public class DSM2_generator {
	//private static final Logger log = LogManager.getLogger();
	private HalfGrid halfGrid;

	public DSM2_generator(Rect rect, Vec<GeoPoint> points) {		
		this.halfGrid = new HalfGrid(rect);
		halfGrid.insertMax(points);
	}

	public RasterGrid get() {
		RasterGrid rasterGrid = halfGrid.getRobust();
		return rasterGrid;
	}
}
