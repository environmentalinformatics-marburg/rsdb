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

public interface JTSValueGeometryConsumer {

	default void acceptPolygon(int value, Polygon polygon) {
		//Logger.info("Polygon");
	}

	default void acceptMultiPolygon(int value, MultiPolygon multiPolygon) {
		final int n = multiPolygon.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Polygon polygon = ((Polygon) multiPolygon.getGeometryN(i));
			acceptPolygon(value, polygon);
		}
	}
	
	default void acceptLineString(int value, LineString lineString) {
		//Logger.info("LineString");
	}
	
	default void acceptMultiLineString(int value, MultiLineString multiLineString) {
		final int n = multiLineString.getNumGeometries();
		for (int i = 0; i < n; i++) {
			LineString lineString = ((LineString) multiLineString.getGeometryN(i));
			acceptLineString(value, lineString);
		}
	}

	default void acceptMultiPoint(int value, MultiPoint multiPoint) {
		final int n = multiPoint.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Point point = ((Point) multiPoint.getGeometryN(i));
			acceptPoint(value, point);
		}
	}

	default void acceptLinearRing(int value, LinearRing linearRing) {
		acceptLineString(value, linearRing);
	}

	default void acceptPoint(int value, Point point) {
		//Logger.info("Point");
	}

	default void acceptGeometryCollection(int value, GeometryCollection geometryCollection) {
		final int n = geometryCollection.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Geometry geometry = geometryCollection.getGeometryN(i);
			acceptGeometry(value, geometry);
		}
	}

	default void acceptGeometry(int value, Geometry geometry) {
		if(geometry instanceof Polygon) {
			acceptPolygon(value, (Polygon) geometry);
		} else if(geometry instanceof MultiPolygon) {
			acceptMultiPolygon(value, (MultiPolygon) geometry);
		}  else if(geometry instanceof GeometryCollection) {
			acceptGeometryCollection(value, (GeometryCollection) geometry);
		}  else if(geometry instanceof LineString) {
			acceptLineString(value, (LineString) geometry);			
		}  else if(geometry instanceof Point) {
			acceptPoint(value, (Point) geometry);			
		}  else if(geometry instanceof LinearRing) {
			acceptLinearRing(value, (LinearRing) geometry);			
		}  else if(geometry instanceof MultiPoint) {
			acceptMultiPoint(value, (MultiPoint) geometry);			
		}  else if(geometry instanceof MultiLineString) {
			acceptMultiLineString(value, (MultiLineString) geometry);
		} else {
			Logger.info("unknown geometry: " + geometry.getClass());
		}
	}
}