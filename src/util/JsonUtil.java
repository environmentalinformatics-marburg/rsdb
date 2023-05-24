package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import util.collections.ReadonlyList;
import util.collections.vec.Vec;
import util.yaml.YamlMap.BooleanConsumer;

public class JsonUtil {

	public static ReadonlyList<String> optStringReadonlyList(JSONObject json, String key) {
		return ReadonlyList.of(optStringArray(json, key));
	}

	public static Vec<String> optStringList(JSONObject json, String key) {
		Vec<String> list = new Vec<String>();
		if(json.has(key)) {
			JSONArray optArray = json.optJSONArray(key);
			if(optArray == null) {
				String optString = json.optString(key, null);
				if(optString == null) {
					throw new RuntimeException();
				} else {
					list.add(optString);
				}
			} else {
				int len = optArray.length();
				for (int i = 0; i < len; i++) {
					String optString = optArray.optString(i, null);
					if(optString == null) {
						throw new RuntimeException();
					} else {
						list.add(optString);
					}
				}
			}
		}
		return list;
	}

	public static Vec<String> optStringTrimmedList(JSONObject json, String key) {
		Vec<String> list = new Vec<String>();
		if(json.has(key)) {
			JSONArray optArray = json.optJSONArray(key);
			if(optArray == null) {
				String optString = json.optString(key, null);
				if(optString == null) {
					throw new RuntimeException();
				} else {
					list.add(optString.trim());
				}
			} else {
				int len = optArray.length();
				for (int i = 0; i < len; i++) {
					String optString = optArray.optString(i, null);
					if(optString == null) {
						throw new RuntimeException();
					} else {
						list.add(optString.trim());
					}
				}
			}
		}
		return list;
	}

	public static String[] optStringArray(JSONObject json, String key) {
		return optStringList(json, key).toArray(new String[0]);
	}

	public static String[] optStringTrimmedArray(JSONObject json, String key) {
		return optStringTrimmedList(json, key).toArray(new String[0]);
	}

	public static int[] getIntArray(JSONObject json, String key) {
		JSONArray jsonArray = json.optJSONArray(key);
		if(jsonArray == null) {
			int a0 = getInt(json, key);
			return new int[] {a0};
		}
		int len = jsonArray.length();
		int[] a = new int[len];
		for(int i=0; i<len; i++) {
			a[i] = Integer.parseInt(jsonArray.get(i).toString());
		}
		return a;
	}
	
	public static String[] getStringArray(JSONObject json, String key) {
		JSONArray jsonArray = json.optJSONArray(key);
		if(jsonArray == null) {
			String a0 = getString(json, key);
			return new String[] {a0};
		}
		int len = jsonArray.length();
		String[] a = new String[len];
		for(int i=0; i<len; i++) {
			a[i] = jsonArray.get(i).toString();
		}
		return a;
	}

	public static int getInt(JSONObject json, String key) {
		return Integer.parseInt(json.get(key).toString());
	}

	public static String getString(JSONObject json, String key)  {
		return json.get(key).toString();
	}

	public static Vec<Integer> getIntegerVec(JSONObject json, String key) {
		JSONArray jsonArray = json.getJSONArray(key);
		int len = jsonArray.length();
		Integer[] a = new Integer[len];
		for(int i=0; i<len; i++) {
			a[i] = Integer.parseInt(jsonArray.get(i).toString());
		}
		return new Vec<Integer>(a);
	}

	public static double[] getDoubleArray(JSONObject json, String key) {
		JSONArray jsonArray = json.getJSONArray(key);
		int len = jsonArray.length();
		double[] a = new double[len];
		for(int i=0; i<len; i++) {
			a[i] = Double.parseDouble(jsonArray.get(i).toString());
		}
		return a;
	}

	public static double getDouble(JSONObject json, String key) {
		if(json.get(key).toString().trim().isEmpty()) {
			return Double.NaN;
		}
		double v = json.getDouble(key);
		return Double.isFinite(v) ? v : Double.NaN;
	}

	public static void writeOptList(JSONWriter json, String key, List<?> list) {
		if(list.isEmpty()) {
			return;
		}
		if(list.size() == 1) {
			json.key(key);
			json.value(list.get(0));
			return;
		}
		json.key(key);
		json.array();
		for(Object e:list) {
			json.value(e);
		}
		json.endArray();
	}

	public static void writeOptArray(JSONWriter json, String key, Object[] a) {
		if(a==null || a.length == 0) {
			return;
		}
		if(a.length == 1) {
			json.key(key);
			json.value(a[0]);
			return;
		}
		json.key(key);
		json.array();
		for(Object e:a) {
			json.value(e);
		}
		json.endArray();
	}

	public static void put(JSONWriter json, String key, String value) {
		json.key(key);
		json.value(value);
	}

	public static void putFloat(JSONWriter json, String key, float value) {
		json.key(key);
		json.value(value);
	}

	public static void optPut(JSONWriter json, String key, String value) {
		if(value != null && !value.isEmpty()) {
			json.key(key);
			json.value(value);
		}		
	}

	public static void optPutString(JSONWriter json, String key, Object value) {
		if(value != null) {
			optPut(json, key, value.toString());
		}		
	}

	public static void optFunString(JSONObject jsonObject, String name, Consumer<String> fun) {
		String value = jsonObject.optString(name, null);
		if(value != null) {
			fun.accept(value);
		}		
	}

	public static void optFunJSONObject(JSONObject jsonObject, String name, Consumer<JSONObject> fun) {
		JSONObject value = jsonObject.optJSONObject(name);
		if(value != null) {
			fun.accept(value);
		}		
	}

	public static void optFunBoolean(JSONObject jsonObject, String name, BooleanConsumer fun) {
		boolean valueF = jsonObject.optBoolean(name, false);
		boolean valueT = jsonObject.optBoolean(name, true);
		if(valueF == valueT) {
			fun.accept(valueF);
		}		
	}
	
	public static void optFunInt(JSONObject jsonObject, String name, IntConsumer fun) {
		if(jsonObject.has(name)) {
			int value = jsonObject.getInt(name);
			fun.accept(value);
		}	
	}

	public static void optFunDouble(JSONObject jsonObject, String name, DoubleConsumer fun) {
		double value = jsonObject.optDouble(name, Double.NaN);
		if(Double.isFinite(value)) {
			fun.accept(value);
		}			
	}

	public static float[] optFloatArray(JSONObject json, String name, float[] optValue) {
		JSONArray jsonArray = json.optJSONArray(name);
		if(jsonArray == null) {
			float v = json.optFloat(name);
			return Float.isFinite(v) ? new float[] {v} : optValue;
		}
		int len = jsonArray.length();
		if(len == 0) {
			return optValue;
		}
		float[] vs = new float[len];
		for (int i = 0; i < len; i++) {
			float v = jsonArray.optFloat(i);
			if(!Float.isFinite(v)) {
				throw new RuntimeException("wrong value in float array");
			}
			vs[i] = v;
		}
		return vs;
	}

	public static void putFloatArray(JSONWriter json, String name, float[] values) {
		json.key(name);
		json.array();
		for(float value : values) {
			json.value(value);
		}
		json.endArray();		
	}
	
	public static <T> HashMap<String, T> parsePropertyHashMap(JSONObject json, Function<JSONObject, T> parser) {
		HashMap<String, T> collectorMap = new HashMap<String, T>();
		parsePropertyMap(json, parser, collectorMap);
		return collectorMap;
	}
	
	public static <T> void parsePropertyMap(JSONObject json, Function<JSONObject, T> parser, Map<String, T> collectorMap) {
		Iterator<String> it = json.keys();
		while(it.hasNext()) {
			String key = it.next();	
			JSONObject innerJSON = json.getJSONObject(key);
			T property = parser.apply(innerJSON);
			collectorMap.put(key, property);			
		}
	}
}
