package server.api.pointdb.feature;

import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.Rect;
import pointdb.processing.tilepoint.Counter;

public class Feature_point_count extends Feature {

	@Override
	public void calc(JSONWriter json, PointDB db, Rect rect) {
		Counter counter = new Counter(db.tilePointProducer(rect));
		json.object();
		json.key("point_count");
		json.value(counter.count());
		json.endObject();		
	}

}
