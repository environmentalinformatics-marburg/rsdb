package server.api.pointdb;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.RasterGrid;
import util.collections.vec.Vec;

public class DTM2_generator {
	//private static final Logger log = LogManager.getLogger();

	HalfGrid halfGrid;

	public DTM2_generator(Rect rect, Vec<GeoPoint> points) {
		halfGrid = new HalfGrid(rect);
		halfGrid.insertMin(points);
	}

	public RasterGrid get() {
		
		//halfGrid.removePeaks();
		halfGrid.retainMostLikely();
		halfGrid.regCheck();
		//halfGrid.removeNoneGrounds();
		//halfGrid.removeNoneGrounds();
		return halfGrid.get();
	}
}
