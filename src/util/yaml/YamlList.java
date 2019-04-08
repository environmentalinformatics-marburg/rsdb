package util.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import util.collections.ReadonlyList;
import util.collections.array.ReadonlyArray;

public class YamlList {
	
	private List<Object> list;
	
	public YamlList(Object data) {
		if(data instanceof List) {
			this.list = (List<Object>) data;
		} else {
			this.list = new ArrayList<Object>();
			this.list.add(data);
		}
	}
	
	public YamlList(List<Object> list) {
		this.list = list;
	}
	
	public List<YamlMap> asMaps() {
		ArrayList<YamlMap> result = new ArrayList<YamlMap>(list.size());
		for(Object e:list) {
			if(e instanceof Map) {
				result.add(new YamlMap((Map<String, Object>) e));
			} else {
				throw new RuntimeException("element is no map "+e);
			}
		}
		return result;
	}
	
	public List<String> asStrings() {
		ArrayList<String> result = new ArrayList<String>(list.size());
		for(Object e:list) {
			result.add(e.toString());
		}
		return result;
	}
	
	public String[] asStringArray() {
		return asStrings().toArray(new String[0]);
	}
	
	public ReadonlyArray<String> asReadonlyStrings() {
		return ReadonlyList.of(asStringArray());
	}

	public float[] asFloatArray() {
		float[] result = new float[list.size()];
		for (int i = 0; i < result.length; i++) {
			Object o = list.get(i);
			if(o instanceof Number) {
				result[i] = ((Number) o).floatValue();
			} else {
				throw new RuntimeException("element is not a number: "+o);
			}
		}
		return result;
	}
}
