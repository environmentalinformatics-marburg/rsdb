package server.api.pointdb.feature;

import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.Rect;
import pointdb.processing.tilepoint.Counter;
import pointdb.processing.tilepoint.PointFilter;

public class Feature_pulse_count extends Feature {

	@Override
	public void calc(JSONWriter json, PointDB db, Rect rect) {
		Counter counter = new Counter(db.tilePointProducer(rect).filter(PointFilter.createAtomicFilter("last_return=1")));
		json.object();
		json.key("pulse_count");
		json.value(counter.count());
		json.endObject();		
	}

}
