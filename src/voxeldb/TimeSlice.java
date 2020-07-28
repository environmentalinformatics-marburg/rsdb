package voxeldb;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import util.yaml.YamlMap;

public class TimeSlice {
	
	public static class TimeSliceBuilder {
		public String name;

		public TimeSliceBuilder(String name) {
			this.name = name;
		}
	}
	
	public final int t;
	public final String name;
	
	public TimeSlice(int t, String name) {
		Objects.requireNonNull(name);
		this.t = t;
		this.name = name;
	}
	
	public TimeSlice(int t, TimeSliceBuilder timeSliceBuilder) {
		this(t, timeSliceBuilder.name);
	}

	public Map<String, Object> toYamlWithoutT() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		return map;
	}
	
	public static TimeSlice ofYamlWithT(YamlMap yamlMap, int t) {
		String name = yamlMap.optString("name", "");
		return new TimeSlice(t, name);
	}
	
	public boolean hasName() {
		return !name.isEmpty();
	}
	
	public static LinkedHashMap<String, Object> timeMapToYaml(Map<Integer, TimeSlice> timeMap) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		for(TimeSlice timeSlice:timeMap.values()) {
			map.put(Integer.toString(timeSlice.t), timeSlice.toYamlWithoutT());
		}
		return map;
	}
	
	public static void yamlToTimeMap(Map<Number, Object> yamlMap, Map<Integer, TimeSlice> timeMap) {
		for (Entry<Number, Object> entry : yamlMap.entrySet()) {
			int t = entry.getKey().intValue();
			YamlMap ts = YamlMap.ofObject(entry.getValue());
			TimeSlice timeslice = TimeSlice.ofYamlWithT(ts, t);
			timeMap.put(timeslice.t, timeslice);
		}
	}
}
