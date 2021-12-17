package server.api.pointdb.feature;


import org.tinylog.Logger;
import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.GeoPointProducer;
import pointdb.processing.geopoint.Normalise;
import util.collections.vec.Vec;

public class Feature_canopy_height extends Feature {
	

	@Override
	public void calc(JSONWriter json, PointDB db, Rect rect) {
		
		
		Normalise normalise = new Normalise();
		normalise.normalise_extremes = true;
		normalise.normalise_ground = true;
		normalise.normalise_origin = true;
		

		GeoPointProducer producer = db.tilePointProducer(rect).toGeoPointProducer();
		if(normalise.normalise_origin) {
			producer = producer.transform(-rect.getUTMd_min_x(), -rect.getUTMd_min_y());
		}
		Vec<GeoPoint> result = normalise.optional_normalise(producer.toList());
		
		double max_z=0;
		for(GeoPoint p:result) {
			if(max_z<p.z) {
				max_z = p.z;
			}
		}
		
		Logger.info("points "+result.size());
		
		if(max_z<=0) {
			throw new RuntimeException("canopy_height calculation not possible for "+rect);
		}
		
		

		json.object();
		json.key("canopy_height");
		json.value(max_z);
		json.endObject();
		
	}

}
