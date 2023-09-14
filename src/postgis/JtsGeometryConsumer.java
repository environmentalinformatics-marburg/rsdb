package postgis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.tinylog.Logger;

public interface JtsGeometryConsumer {

	default void acceptPolygon(Polygon polygon) {
		//Logger.info("Polygon");
	}

	default void acceptMultiPolygon(MultiPolygon multiPolygon) {
		final int n = multiPolygon.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Polygon polygon = ((Polygon) multiPolygon.getGeometryN(i));
			acceptPolygon(polygon);
		}
	}
	
	default void acceptLineString(LineString lineString) {
		//Logger.info("LineString");
	}
	
	default void acceptMultiLineString(MultiLineString multiLineString) {
		final int n = multiLineString.getNumGeometries();
		for (int i = 0; i < n; i++) {
			LineString lineString = ((LineString) multiLineString.getGeometryN(i));
			acceptLineString(lineString);
		}
	}

	default void acceptMultiPoint(MultiPoint multiPoint) {
		final int n = multiPoint.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Point point = ((Point) multiPoint.getGeometryN(i));
			acceptPoint(point);
		}
	}

	default void acceptLinearRing(LinearRing linearRing) {
		acceptLineString(linearRing);
	}

	default void acceptPoint(Point point) {
		//Logger.info("Point");
	}

	default void acceptGeometryCollection(GeometryCollection geometryCollection) {
		final int n = geometryCollection.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Geometry geometry = geometryCollection.getGeometryN(i);
			acceptGeometry(geometry);
		}
	}

	default void acceptGeometry(Geometry geometry) {
		if(geometry instanceof Polygon) {
			acceptPolygon((Polygon) geometry);
		} else if(geometry instanceof MultiPolygon) {
			acceptMultiPolygon((MultiPolygon) geometry);
		}  else if(geometry instanceof GeometryCollection) {
			acceptGeometryCollection((GeometryCollection) geometry);
		}  else if(geometry instanceof LineString) {
			acceptLineString((LineString) geometry);			
		}  else if(geometry instanceof Point) {
			acceptPoint((Point) geometry);			
		}  else if(geometry instanceof LinearRing) {
			acceptLinearRing((LinearRing) geometry);			
		}  else if(geometry instanceof MultiPoint) {
			acceptMultiPoint((MultiPoint) geometry);			
		}  else if(geometry instanceof MultiLineString) {
			acceptMultiLineString((MultiLineString) geometry);
		} else {
			Logger.info("unknown geometry: " + geometry.getClass());
		}
	}
}