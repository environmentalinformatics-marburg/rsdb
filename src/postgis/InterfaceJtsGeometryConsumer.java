package postgis;

import java.util.function.Consumer;

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

public interface InterfaceJtsGeometryConsumer extends Consumer<Geometry> {

	void acceptPolygon(Polygon polygon);

	void acceptMultiPolygon(MultiPolygon multiPolygon);
	
	void acceptLineString(LineString lineString);
	
	void acceptMultiLineString(MultiLineString multiLineString);

	void acceptMultiPoint(MultiPoint multiPoint);

	void acceptLinearRing(LinearRing linearRing);

	void acceptPoint(Point point);

	void acceptGeometryCollection(GeometryCollection geometryCollection);

	default void accept(Geometry geometry) {
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