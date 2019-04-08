package pointdb.processing.geopoint;

import pointdb.base.GeoPoint;
import pointdb.base.Point2d;
import pointdb.base.Rect;
import util.collections.vec.Vec;

public interface GeoPointProducer {
	
	public void produce(GeoPointConsumer geoPointConsumer);
	
	public default GeoPointProducer clip(Point2d[] polygon) {
		return new PolygonClippingProcessor(this,polygon);
	}
	
	public default GeoPointProducer transform(double tx, double ty) {
		return new GeoPointTransformProcessor(this, tx, ty);
	}
	
	public default Vec<GeoPoint> toList() {
		return GeoPointCollector.toList(this);
	}
	
	public default PointGrid toPointGrid(Rect rect) {
		PointGrid pointGrid = new PointGrid(rect);
		this.produce(pointGrid);
		return pointGrid;
	}
	
	public default PointGrid toPointGrid(Rect rect, int initialCellCapacity) {
		PointGrid pointGrid = new PointGrid(rect,initialCellCapacity);
		this.produce(pointGrid);
		return pointGrid;
	}	
}
