package server.api.pointdb;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.PointGrid;
import pointdb.processing.geopoint.RasterGrid;
import util.collections.vec.Vec;

public class DTM_generator {
	
	public static RasterGrid generate(Vec<GeoPoint> rawPoints, Rect rect) {
		PointGrid pointGrid = PointGrid.of(rect, rawPoints);
		return generate(pointGrid);
	}
	
	public static RasterGrid generate(PointGrid pointGrid) {
		/*pointGrid.removeNegativeOutliers();
		//pointGrid.groundingSimple();
		pointGrid.grounding(0.6d);
		//pointGrid.grounding(0.3d);
		//pointGrid.groundingSimple();
		RasterGrid rasterGrid = RasterGrid.of(pointGrid);
		rasterGrid.window_z(pointGrid, 8, 16);		
		return rasterGrid;*/		
		
		pointGrid.removeNegativeOutliers();
		//pointGrid.retainGround();
		pointGrid.grounding(0.6d);
		//pointGrid.grounding(0.1d);
		RasterGrid rasterGrid = RasterGrid.ofExtent(pointGrid);
		rasterGrid.window_z(pointGrid, 8, 16);		
		return rasterGrid;
	}

}
