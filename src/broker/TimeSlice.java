package broker;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.json.JSONWriter;

import util.yaml.YamlMap;

public class TimeSlice {

	public static class TimeSliceBuilder {
		public String name;

		public TimeSliceBuilder(String name) {
			this.name = name;
		}
	}

	public final int id;
	public final String name;

	public TimeSlice(int id, String name) {
		Objects.requireNonNull(name);
		this.id = id;
		this.name = name;
	}

	public TimeSlice(int id, TimeSliceBuilder timeSliceBuilder) {
		this(id, timeSliceBuilder.name);
	}

	public Map<String, Object> toYamlWithoutT() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		return map;
	}

	public static TimeSlice ofYamlWithId(YamlMap yamlMap, int id) {
		String name = yamlMap.optString("name", "");
		return new TimeSlice(id, name);
	}

	public boolean hasName() {
		return !name.isEmpty();
	}

	public static LinkedHashMap<Integer, Object> timeMapToYaml(Map<Integer, TimeSlice> timeMap) {
		LinkedHashMap<Integer, Object> map = new LinkedHashMap<Integer, Object>();
		for(TimeSlice timeSlice:timeMap.values()) {
			map.put(timeSlice.id, timeSlice.toYamlWithoutT());
		}
		return map;
	}

	public static void yamlToTimeMap(Map<?, Object> yamlMap, Map<Integer, TimeSlice> timeMap) {
		for (Entry<?, Object> entry : yamlMap.entrySet()) {
			Object key = entry.getKey();
			int id = key instanceof Number ? ((Number)key).intValue() : Integer.parseInt(key.toString());
			YamlMap ts = YamlMap.ofObject(entry.getValue());
			TimeSlice timeslice = TimeSlice.ofYamlWithId(ts, id);
			timeMap.put(timeslice.id, timeslice);
		}
	}

	public static void timeSlicesToJSON(Collection<TimeSlice> timeSlices, JSONWriter json) {
		json.array();
		for(TimeSlice timeSlice : timeSlices) {
			timeSlice.toJSON(json);
		}
		json.endArray();
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("id");
		json.value(id);
		if(hasName()) {
			json.key("name");
			json.value(name);
		}
		json.endObject();
	}
}
