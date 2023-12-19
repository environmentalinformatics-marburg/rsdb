package broker.group;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tinylog.Logger;

import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.PolygonUtil.PolygonWithHoles;
import util.Util;
import util.collections.vec.Vec;

public class Roi {

	public final String name;
	//public final Point2d[] points;
	public final PolygonWithHoles[] polygons;
	public final Point2d center;
	
	public Roi(String name, Point2d[] points) {
		this(name, new PolygonWithHoles[] {PolygonWithHoles.ofPolygon(points)});
	}

	public Roi(String name, PolygonWithHoles[] polygons) {
		PolygonUtil.PolygonsWithHoles.validatePolygonsWithHoles(polygons);	
		this.name = name;
		this.polygons = polygons;
		this.center = PolygonUtil.PolygonsWithHoles.centroidOfPolygonsWithHolesPoints(polygons);
	}

	public static Roi[] readRoiGeoJSON(Path filename) throws IOException {
		//Logger.info("read "+filename);
		String jsonText = new String(Files.readAllBytes(filename), StandardCharsets.UTF_8);
		JSONObject json = new JSONObject(jsonText);
		if(!json.getString("type").equals("FeatureCollection")) {
			throw new RuntimeException("type expected 'FeatureCollection' found: "+json.getString("type")+" in "+filename);
		}
		JSONArray features = json.getJSONArray("features");
		Vec<Roi> rois = new Vec<Roi>();
		int feature_count = features.length();
		for (int i = 0; i < feature_count; i++) {
			try {
				JSONObject feature = features.getJSONObject(i);
				String name = feature.getJSONObject("properties").getString("name");

				JSONObject geometry = feature.getJSONObject("geometry");
				if(!geometry.getString("type").equals("Polygon")) {
					throw new RuntimeException("type expected 'Polygon' found: "+geometry.getString("type")+" in "+filename);
				}
				JSONArray over_coordinates = geometry.getJSONArray("coordinates");
				if(over_coordinates.length()!=1) {
					throw new RuntimeException("one ROI needs to consist of exactly one polygon: "+over_coordinates.length()+"  "+feature+"  in "+filename);
				}
				JSONArray coordinates = over_coordinates.getJSONArray(0);
				int coordinates_count = coordinates.length();
				if(coordinates_count<4) {
					throw new RuntimeException("one polygon needs to consist of at least four points (with same coordinates of first and last point): "+coordinates_count+"  "+feature+"  in "+filename);
				}
				Vec<Point2d> points = new Vec<Point2d>();
				for (int j = 0; j < coordinates_count; j++) {
					JSONArray p = coordinates.getJSONArray(j);
					if(p.length()!=2) {
						throw new RuntimeException("one point needs to consist of exactly two coordinates: "+p.length()+"  "+feature+"  in "+filename);
					}
					double x = p.getDouble(0);
					double y = p.getDouble(1);
					Point2d point = new Point2d(x, y);
					points.add(point);
				}
				if(Util.isValidID(name)) {
					Roi roi = new Roi(name, points.toArray(new Point2d[0]));
					//Logger.info(roi);
					rois.add(roi);
				} else {
					Logger.warn("ROI not inserted: invalid identifier: "+name+" in "+filename);
				}
			} catch(Exception e) {
				Logger.error(e);
			}
		}
		return rois.toArray(new Roi[0]);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ROI");
		builder.append(" ");
		builder.append(name);
		builder.append("  center (");
		builder.append(center.x);
		builder.append(", ");
		builder.append(center.x);
		builder.append(") ");
		/*builder.append(" [");
		for(Point2d point:points) {
			builder.append(" ");
			builder.append(point.x);
			builder.append(", ");
			builder.append(point.y);
			builder.append(" ");
		}
		builder.append("]");*/
		return builder.toString();
	}
}
