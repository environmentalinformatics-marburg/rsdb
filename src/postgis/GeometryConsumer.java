package postgis;

import org.tinylog.Logger;

import net.postgis.jdbc.geometry.Geometry;
import net.postgis.jdbc.geometry.MultiPolygon;
import net.postgis.jdbc.geometry.Polygon;

public interface GeometryConsumer {

	void acceptPolygon(Polygon polygon);

	default void acceptMultiPolygon(MultiPolygon multiPolygon) {
		Polygon[] polygons = multiPolygon.getPolygons();
		for(Polygon polygon : polygons) {
			acceptPolygon(polygon);
		}
	}

	default void acceptGeometry(Geometry geometry) {
		if(geometry instanceof Polygon) {
			acceptPolygon((Polygon) geometry);
		} else if(geometry instanceof MultiPolygon) {
			acceptMultiPolygon((MultiPolygon) geometry);
		} else {
			Logger.info("unknown geometry: " + geometry.getClass());
		}
	}
}