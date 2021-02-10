package broker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;

public class InformalProperties {
	private Map<String, ReadonlyArray<String>> map;

	public InformalProperties(Builder builder) {
		map = new HashMap<String, ReadonlyArray<String>>();
		builder.map.forEach((tag, contents) -> {
			map.put(tag, contents.copyReadonly());
		});
	}

	public void writeJson(JSONWriter json) {
		json.object();
		map.forEach((tag, contents) -> {
			json.key(tag);
			json.array();
			for(String content : contents) {
				json.value(content);
			}
			json.endArray();
		});
		json.endObject();
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		map.forEach((tag, contents) -> {
			int len = contents.size();
			if(len == 0) {
				// nothing
			} else if(len == 1){
				yamlMap.put(tag, contents.first());
			} else {
				yamlMap.put(tag, contents);
			}
		});	
		return yamlMap;
	}

	public void forEachTag(BiConsumer<String, ReadonlyArray<String>> consumer) {
		map.forEach(consumer);			
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public static class Builder {
		public Map<String, Vec<String>> map = new HashMap<String, Vec<String>>();

		public Builder() {
		}

		public Builder(InformalProperties properties) {
			properties.forEachTag((tag, contents) -> {
				this.map.put(tag, contents.copyVec());
			});
		}

		public InformalProperties build() {
			return new InformalProperties(this);
		}

		public static Builder ofJSON(JSONObject jsonProperties) {
			Builder builder = new Builder();
			Iterator<String> it = jsonProperties.keys();
			while(it.hasNext()) {
				String key = it.next();
				JSONArray jsonArray = jsonProperties.getJSONArray(key);
				int len = jsonArray.length();
				Vec<String> vec = new Vec<String>(len);
				for (int i = 0; i < len; i++) {
					String value = jsonArray.getString(i);
					vec.add(value);					
				}
				builder.map.put(key, vec);
			}
			return builder;
		}
	}
}