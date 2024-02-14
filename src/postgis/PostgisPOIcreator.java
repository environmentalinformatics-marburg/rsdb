package postgis;

import java.util.TreeSet;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import broker.group.Poi;
import postgis.PostgisLayer.ObjectJtsGeometryConsumer;
import util.collections.vec.Vec;

public class PostgisPOIcreator {

	public static class POIjtsGeometryConsumer implements InterfaceJtsGeometryConsumer, ObjectJtsGeometryConsumer {

		public Vec<Poi> pois = new Vec<Poi>();
		private UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator();
		private Vec<String> messages;

		protected String currentPoiName = null; // nullable

		public POIjtsGeometryConsumer(Vec<String> messages) {
			this.messages = messages;
		}

		@Override
		public void acceptPolygon(Polygon polygon) {
			messages.add("Skip POI: no point: Polygon");		
		}

		@Override
		public void acceptMultiPolygon(MultiPolygon multiPolygon) {
			messages.add("Skip POI: no point: MultiPolygon");			
		}

		@Override
		public void acceptLineString(LineString lineString) {
			messages.add("Skip POI: no point: LineString");			
		}

		@Override
		public void acceptMultiLineString(MultiLineString multiLineString) {
			messages.add("Skip POI: no point: MultiLineString");			
		}

		@Override
		public void acceptMultiPoint(MultiPoint multiPoint) {
			acceptGeometryCollection(multiPoint);
		}

		@Override
		public void acceptLinearRing(LinearRing linearRing) {
			messages.add("Skip POI: no point: LinearRing");			
		}

		@Override
		public void acceptPoint(Point point) {
			Coordinate c = point.getCoordinate();
			String name = createUniqueName();
			pois.add(new Poi(name, c.x, c.y));
		}

		@Override
		public void acceptGeometryCollection(GeometryCollection geometryCollection) {
			if(geometryCollection.getNumGeometries() == 1) {
				Geometry geometry = geometryCollection.getGeometryN(0);
				accept(geometry);
			} else {
				messages.add("Skip POI: no point: Multiple geometries");
			}			
		}

		private String createUniqueName() {
			return uniqueNameGenerator.createUniqueName(currentPoiName, "poi");
		}

		@Override
		public void accept(Object value, Geometry geometry) {
			currentPoiName = value == null ? null : value.toString();
			accept(geometry);
		}
	}

	public static Vec<Poi> create(PostgisLayer postgisLayer, Vec<String> messages) {
		POIjtsGeometryConsumer poijtsGeometryConsumer = new POIjtsGeometryConsumer(messages);
		TreeSet<String> geoTypes = new TreeSet<String>(postgisLayer.getGeometryTypes());
		if(geoTypes.contains("Point") || geoTypes.contains("MultiPoint") || geoTypes.contains("GeometryCollection")) {
			try {
				String nameField = postgisLayer.getNameField();
				if(nameField.isBlank()) {
					postgisLayer.forEachJtsGeometry(null, 0, false, null, poijtsGeometryConsumer);
				} else {
					postgisLayer.forEachObjectJtsGeometry(null, false, nameField, poijtsGeometryConsumer);
				}		
			} catch(Exception e) {
				messages.add("Skip POIs: " + e.getMessage());
			}
		} else {
			messages.add("Skip POIs: no point geometries: " + geoTypes);			
		}
		return poijtsGeometryConsumer.pois;
	}
}
