package postgis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public interface InterfaceDecomposeMultiJtsGeometryConsumer extends InterfaceJtsGeometryConsumer {
	
	default void acceptMultiPolygon(MultiPolygon multiPolygon) {
		final int n = multiPolygon.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Polygon polygon = ((Polygon) multiPolygon.getGeometryN(i));
			acceptPolygon(polygon);
		}
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

	default void acceptGeometryCollection(GeometryCollection geometryCollection) {
		final int n = geometryCollection.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Geometry geometry = geometryCollection.getGeometryN(i);
			accept(geometry);
		}
	}
}
