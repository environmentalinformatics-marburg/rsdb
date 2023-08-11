package postgis;

import org.tinylog.Logger;

public interface JTSGeometryConsumer {

	void acceptPolygon(org.locationtech.jts.geom.Polygon polygon);

	default void acceptMultiPolygon(org.locationtech.jts.geom.MultiPolygon multiPolygon) {
		final int n = multiPolygon.getNumGeometries();
		for (int i = 0; i < n; i++) {
			org.locationtech.jts.geom.Polygon polygon = ((org.locationtech.jts.geom.Polygon)multiPolygon.getGeometryN(0));
			acceptPolygon(polygon);
		}
	}

	default void acceptGeometry(org.locationtech.jts.geom.Geometry geometry) {
		if(geometry instanceof org.locationtech.jts.geom.Polygon) {
			acceptPolygon((org.locationtech.jts.geom.Polygon) geometry);
		} else if(geometry instanceof org.locationtech.jts.geom.MultiPolygon) {
			acceptMultiPolygon((org.locationtech.jts.geom.MultiPolygon) geometry);
		} else {
			Logger.info("unknown geometry: " + geometry.getClass());
		}
	}
}