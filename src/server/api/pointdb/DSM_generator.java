package server.api.pointdb;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.PointGrid;
import pointdb.processing.geopoint.RasterGrid;
import util.collections.vec.Vec;

public class DSM_generator {

	public static RasterGrid generate(Vec<GeoPoint> rawPoints, Rect rect) {
		PointGrid pointGrid = PointGrid.of(rect, rawPoints);
		return generate(pointGrid);
	}


	public static RasterGrid generate(PointGrid pointGrid) {		
		/*//pointGrid_DSM.removePositiveOutliers();
		//pointGrid_DSM.retainMax();
		pointGrid.retainTop();
		RasterGrid rasterGrid_DSM = RasterGrid.of(pointGrid);
		//rasterGrid_DSM.window_z(pointGrid_DSM, 4, 16);
		rasterGrid_DSM.first_z(pointGrid);		
		return rasterGrid_DSM;*/
		
		//pointGrid.sortReverseZ();
		pointGrid.retainTop();
		RasterGrid rasterGrid = RasterGrid.ofExtent(pointGrid);
		rasterGrid.first_z(pointGrid);
		//rasterGrid.window_z(pointGrid, 16, 16);
		return rasterGrid;
	}

}
