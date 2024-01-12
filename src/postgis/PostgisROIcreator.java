package postgis;

import java.util.HashSet;

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

import broker.group.Roi;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil.PolygonWithHoles;
import postgis.PostgisLayer.ObjectJtsGeometryConsumer;
import util.collections.vec.Vec;

public class PostgisROIcreator {

	public static class ROIjtsGeometryConsumer implements InterfaceJtsGeometryConsumer, ObjectJtsGeometryConsumer {

		public Vec<Roi> rois = new Vec<Roi>();
		HashSet<String> roiNames = new HashSet<String>();
		Vec<String> messages;

		protected String currentRoiName = null; // nullable

		public ROIjtsGeometryConsumer(Vec<String> messages) {
			this.messages = messages;
		}

		private static Point2d[] linearRingToPoints(LinearRing linearRing) {
			Coordinate[] coordinates = linearRing.getCoordinates();
			Point2d[] points = new Point2d[coordinates.length];
			for (int i = 0; i < coordinates.length; i++) {
				Coordinate c = coordinates[i];
				points[i] = new Point2d(c.x, c.y);
			}
			return points;
		}

		private static PolygonWithHoles jtsPolygonToPolygonWithHoles(Polygon polygon) {
			Point2d[] ps = linearRingToPoints(polygon.getExteriorRing());
			int interiorRingCount = polygon.getNumInteriorRing();
			if(interiorRingCount == 0) {
				return PolygonWithHoles.ofPolygon(ps);
			} else {
				Point2d[][] ips = new Point2d[interiorRingCount][]; 
				for (int i = 0; i < interiorRingCount; i++) {
					ips[i] = linearRingToPoints(polygon.getInteriorRingN(i));
				}
				return new PolygonWithHoles(ps, ips);
			}
		}

		private static PolygonWithHoles[] jtsMultiPolygonToPolygonWithHoles(MultiPolygon multiPolygon) {
			final int n = multiPolygon.getNumGeometries();
			PolygonWithHoles[] pwhs = new PolygonWithHoles[n];
			for (int i = 0; i < n; i++) {
				Polygon polygon = ((Polygon) multiPolygon.getGeometryN(i));
				pwhs[i] = jtsPolygonToPolygonWithHoles(polygon);
			}
			return pwhs;
		}

		@Override
		public void acceptPolygon(Polygon polygon) {
			PolygonWithHoles polygonWithHoles = jtsPolygonToPolygonWithHoles(polygon);
			add(new PolygonWithHoles[] {polygonWithHoles});
		}

		@Override
		public void acceptMultiPolygon(MultiPolygon multiPolygon) {
			PolygonWithHoles[] pwhs = jtsMultiPolygonToPolygonWithHoles(multiPolygon);
			add(pwhs);
		}

		@Override
		public void acceptLineString(LineString lineString) {
			messages.add("Skip ROI: no areal: LineString");			
		}

		@Override
		public void acceptMultiLineString(MultiLineString multiLineString) {
			messages.add("Skip ROI: no areal: MultiLineString");			
		}

		@Override
		public void acceptMultiPoint(MultiPoint multiPoint) {
			messages.add("Skip ROI: no areal: MultiPoint");	
		}

		@Override
		public void acceptLinearRing(LinearRing linearRing) {
			messages.add("Skip ROI: no areal: LinearRing");			
		}

		@Override
		public void acceptPoint(Point point) {
			messages.add("Skip ROI: no areal: Point");	
		}

		private static void collectPolygons(GeometryCollection geometryCollection, Vec<PolygonWithHoles> vec) {	
			final int n = geometryCollection.getNumGeometries();
			for (int i = 0; i < n; i++) {
				Geometry geometry = geometryCollection.getGeometryN(i);
				if(geometry instanceof Polygon) {
					vec.add(jtsPolygonToPolygonWithHoles((Polygon) geometry));
				} else if(geometry instanceof MultiPolygon) {
					vec.addAll(jtsMultiPolygonToPolygonWithHoles((MultiPolygon) geometry));
				}  else if(geometry instanceof GeometryCollection) {
					collectPolygons((GeometryCollection) geometry, vec);
				}  else if(geometry instanceof LineString) {
					throw new RuntimeException("no areal: LineString");			
				}  else if(geometry instanceof Point) {
					throw new RuntimeException("no areal: Point");		
				}  else if(geometry instanceof LinearRing) {
					throw new RuntimeException("no areal: LinearRing");			
				}  else if(geometry instanceof MultiPoint) {
					throw new RuntimeException("no areal: MultiPoint");			
				}  else if(geometry instanceof MultiLineString) {
					throw new RuntimeException("no areal: MultiLineString");	
				} else {
					throw new RuntimeException("unknown geometry: " + geometry.getClass());
				}
			}
		}

		@Override
		public void acceptGeometryCollection(GeometryCollection geometryCollection) {
			try {
				Vec<PolygonWithHoles> vec = new Vec<PolygonWithHoles>();
				collectPolygons(geometryCollection, vec);
				PolygonWithHoles[] pwhs = vec.toArray(PolygonWithHoles[]::new);
				add(pwhs);
			} catch(Exception e) {
				messages.add("Skip ROI: " + e.getMessage());	
			}
		}

		private String createUniqueName() {
			String orgName = currentRoiName == null || currentRoiName.isBlank() ? "roi" : currentRoiName;
			String name = orgName;
			int nameIndex = 1;
			while(roiNames.contains(name)) {
				name = orgName + "_" + (++nameIndex);
			}
			roiNames.add(name);
			return name;
		}

		@Override
		public void accept(Object value, Geometry geometry) {
			currentRoiName = value == null ? null : value.toString();
			accept(geometry);
		}

		private void add(PolygonWithHoles[] polygons) {
			try {
				String name = createUniqueName();
				rois.add(new Roi(name, polygons));
			} catch(Exception e) {
				messages.add("Skip ROI: " + e.getMessage());
			}
		}
	}

	public static Vec<Roi> create(PostgisLayer postgisLayer, Vec<String> messages) {
		ROIjtsGeometryConsumer roijtsGeometryConsumer = new ROIjtsGeometryConsumer(messages);
		postgisLayer.forEachJtsGeometry(null, false, null, roijtsGeometryConsumer);
		return roijtsGeometryConsumer.rois;
	}

}
