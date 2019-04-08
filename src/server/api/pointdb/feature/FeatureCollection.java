package server.api.pointdb.feature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FeatureCollection implements Iterable<Feature> {
	private static final Logger log = LogManager.getLogger();
	
	private static final Map<String, Feature> featureMap = new HashMap<String, Feature>();
	
	static {
		add(new Feature_pulse_density());
		add(new Feature_pulse_count());
		add(new Feature_point_density());
		add(new Feature_point_count());
		add(new Feature_area());
		add(new Feature_coverage());
		add(new Feature_canopy_height());
	}	
	
	private static void add(Feature feature) {
		if(featureMap.containsKey(feature.name)) {
			log.warn("overwrite feature name entry "+feature.name);
		}
		featureMap.put(feature.name, feature);
	}

	@Override
	public Iterator<Feature> iterator() {		
		return featureMap.values().iterator();
	}
	
	public Feature getFeature(String name) {
		return featureMap.get(name);
	}
	
	public Feature[] getFeatures() {
		return featureMap.values().stream().sorted((a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.name,b.name)).toArray(Feature[]::new);
	}

}
