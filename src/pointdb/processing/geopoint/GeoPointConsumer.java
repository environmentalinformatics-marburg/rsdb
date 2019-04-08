package pointdb.processing.geopoint;

import pointdb.base.GeoPoint;

public interface GeoPointConsumer {
	void nextGeoPoint(GeoPoint geoPoint);
}
