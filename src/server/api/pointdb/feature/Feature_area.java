package server.api.pointdb.feature;

import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.Rect;

public class Feature_area extends Feature {

	@Override
	public void calc(JSONWriter json, PointDB db, Rect rect) {
		json.object();
		json.key("area");
		json.value(rect.getArea());
		json.endObject();		
	}

}
