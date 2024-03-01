package postgis.style;

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

import vectordb.style.Style;

public interface StyleJtsGeometryConsumer {

	default void acceptPolygon(Style style, Polygon polygon, String label) {
		//Logger.info("Polygon");
	}

	default void acceptMultiPolygon(Style style, MultiPolygon multiPolygon, String label) {
		final int n = multiPolygon.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Polygon polygon = ((Polygon) multiPolygon.getGeometryN(i));
			acceptPolygon(style, polygon, label);
		}
	}
	
	default void acceptLineString(Style style, LineString lineString, String label) {
		//Logger.info("LineString");
	}
	
	default void acceptMultiLineString(Style style, MultiLineString multiLineString, String label) {
		final int n = multiLineString.getNumGeometries();
		for (int i = 0; i < n; i++) {
			LineString lineString = ((LineString) multiLineString.getGeometryN(i));
			acceptLineString(style, lineString, label);
		}
	}

	default void acceptMultiPoint(Style style, MultiPoint multiPoint, String label) {
		final int n = multiPoint.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Point point = ((Point) multiPoint.getGeometryN(i));
			acceptPoint(style, point, label);
		}
	}

	default void acceptLinearRing(Style style, LinearRing linearRing, String label) {
		acceptLineString(style, linearRing, label);
	}

	default void acceptPoint(Style style, Point point, String label) {
		//Logger.info("Point");
	}

	default void acceptGeometryCollection(Style style, GeometryCollection geometryCollection, String label) {
		final int n = geometryCollection.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Geometry geometry = geometryCollection.getGeometryN(i);
			acceptGeometry(style, geometry, label);
		}
	}

	default void acceptGeometry(Style style, Geometry geometry, String label) {
		if(geometry instanceof Polygon) {
			acceptPolygon(style, (Polygon) geometry, label);
		} else if(geometry instanceof MultiPolygon) {
			acceptMultiPolygon(style, (MultiPolygon) geometry, label);
		}  else if(geometry instanceof GeometryCollection) {
			acceptGeometryCollection(style, (GeometryCollection) geometry, label);
		}  else if(geometry instanceof LineString) {
			acceptLineString(style, (LineString) geometry, label);			
		}  else if(geometry instanceof Point) {
			acceptPoint(style, (Point) geometry, label);			
		}  else if(geometry instanceof LinearRing) {
			acceptLinearRing(style, (LinearRing) geometry, label);			
		}  else if(geometry instanceof MultiPoint) {
			acceptMultiPoint(style, (MultiPoint) geometry, label);			
		}  else if(geometry instanceof MultiLineString) {
			acceptMultiLineString(style, (MultiLineString) geometry, label);
		} else {
			Logger.info("unknown geometry: " + geometry.getClass());
		}
	}
}