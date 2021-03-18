package util.yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class YamlMap {
	
	public static final YamlMap EMPTY_MAP = new YamlMap(new HashMap<>());
	
	private Map<String, Object> map;
	
	public YamlMap(Map<String, Object> map) {
		this.map = map;
	}
	
	@SuppressWarnings("unchecked")
	public static YamlMap ofObject(Object map) {
		return new YamlMap((Map<String, Object>) map);
	}
	
	public Object getObject(String name) {
		Object o = map.get(name);
		if(o==null) {
			throw new RuntimeException("element not found "+name);
		}
		return o;
	}
	
	public Object optObject(String name) {
		return map.get(name);
	}

	public Object optObject(String name, Object def) {
		if(contains(name)) {
			return getObject(name);
		}
		return def;
	}	

	
	/**
	 * contains entry, that is not empty
	 * @param name
	 * @return
	 */
	public boolean contains(String name) {
		return map.containsKey(name) && map.get(name)!=null;
	}
		
	public String getString(String name) {
		Object o = getObject(name);
		return o.toString();
	}
	
	public boolean getBoolean(String name) {
		Object o = getObject(name);
		if(o instanceof Boolean) {
			return ((Boolean)o).booleanValue();
		} else {
			throw new RuntimeException("element type is not boolean "+name+"   "+o.toString()+"    "+o.getClass());
		}
	}
	
	@SuppressWarnings("unchecked")
	public YamlMap getMap(String name) {
		Object o = getObject(name);
		if(o instanceof Map) {
			return new YamlMap((Map<String, Object>) o);
		}
		throw new RuntimeException("element is not a map "+name);
	}
	
	public YamlMap optMap(String name) {
		if(contains(name)) {
			return getMap(name);
		}
		return EMPTY_MAP;
	}
	
	public <T> T funMap(String name, Function<YamlMap, T> fun, Supplier<T> optFun) {
		if(contains(name)) {
			return fun.apply(getMap(name));
		}
		return optFun.get();
	}

	@SuppressWarnings("unchecked")
	public YamlList getList(String name) {
		Object o = getObject(name);
		if(o instanceof List) {
			return new YamlList((List<Object>) o);
		}
		//throw new RuntimeException("element is not a list "+name);
		ArrayList<Object> list = new ArrayList<Object>(1);
		list.add(o);
		return new YamlList(list);
	}
	
	public YamlList optList(String name) {
		if(contains(name)) {
			return getList(name);
		}
		return new YamlList(new ArrayList<Object>(0));
	}
	
	public String optString(String name) {
		return optString(name, null);
	}

	public String optString(String name, String def) {
		if(contains(name)) {
			return getString(name);
		}
		return def;
	}
	
	public Number getNumber(String name) {
		Object o = getObject(name);
		if(o instanceof Number) {
			return (Number) o;
		}
		throw new RuntimeException("element is not a number "+name);
	}
	
	public int getInt(String name) {
		return getNumber(name).intValue();
	}

	public int optInt(String name, int def) {
		if(contains(name)) {
			return getInt(name);
		}
		return def;
	}
	
	public long getLong(String name) {
		return getNumber(name).longValue();
	}

	public long optLong(String name, long def) {
		if(contains(name)) {
			return getLong(name);
		}
		return def;
	}
	
	public double getDouble(String name) {
		return getNumber(name).doubleValue();
	}

	public double optDouble(String name) {
		if(contains(name)) {
			return getDouble(name);
		}
		return Double.NaN;
	}
	
	public double optDouble(String name, double def) {
		if(contains(name)) {
			return getDouble(name);
		}
		return def;
	}
	
	public void funDouble(String name, DoubleConsumer fun) {
		if(contains(name)) {
			fun.accept(getDouble(name));
		}		
	}
	
	public void funDouble(String name, DoubleConsumer fun, Consumer<Exception> errFun) {
		if(contains(name)) {
			try {
				fun.accept(getDouble(name));
			} catch (Exception e) {
				errFun.accept(e);
			}
		}		
	}
	
	public float getFloat(String name) {
		return getNumber(name).floatValue();
	}

	public float optFloat(String name) {
		if(contains(name)) {
			return getFloat(name);
		}
		return Float.NaN;
	}

	public float optFloat(String name, float def) {
		if(contains(name)) {
			return getFloat(name);
		}
		return def;
	}

	public Float optFloat(String name, Float def) {
		if(contains(name)) {
			return getFloat(name);
		}
		return def;
	}

	public void optFunString(String name, Consumer<String> fun) {
		if(contains(name)) {
			fun.accept(getString(name));
		}		
	}
	
	/**
	 * If entry name is present as string, convert it to T by conv and apply to fun if conversion != null
	 * @param name
	 * @param conv
	 * @param fun
	 */
	public <T> void optFunStringConv(String name, Function<String, T> conv, Consumer<T> fun) {
		if(contains(name)) {
			T c = conv.apply(getString(name));
			if(c != null) {
				fun.accept(c);
			}
		}		
	}

	public void optFunInt(String name, IntConsumer fun) {
		if(contains(name)) {
			fun.accept(getInt(name));
		}	
	}
	
	public boolean optBoolean(String name, boolean def) {
		if(contains(name)) {
			return getBoolean(name);
		}
		return def;
	}
	
	@FunctionalInterface
	public interface BooleanConsumer {
	    void accept(boolean value);
	    default BooleanConsumer andThen(BooleanConsumer after) {
	        Objects.requireNonNull(after);
	        return (boolean t) -> { accept(t); after.accept(t); };
	    }
	}
	
	public void optFunBoolean(String name, BooleanConsumer fun) {
		if(contains(name)) {
			fun.accept(getBoolean(name));
		}	
	}
	
	public void optFunDouble(String name, DoubleConsumer fun) {
		if(contains(name)) {
			fun.accept(getDouble(name));
		}	
	}
	
	public void funString(String name, Consumer<String> fun) {
		if(contains(name)) {
			fun.accept(getString(name));
		}		
	}
	
	public void optMapFun(String name, Consumer<YamlMap> fun) {
		if(contains(name)) {
			YamlMap m = optMap(name);
			fun.accept(m);
		}
	}
	
	public <T> T optMapConv(String name, Function<YamlMap, T> conv, T def) {
		if(contains(name)) {
			YamlMap m = optMap(name);
			T t = conv.apply(m);
			return t;
		}
		return def;
	}
	
	public Set<String> keys() {
		return map.keySet();
	}
	
	@Override
	public String toString() {
		return map.toString();
	}
	
	public void forEachKey(BiConsumer<YamlMap, String> consumer) {
		map.keySet().forEach(key -> consumer.accept(this, key));
	}
}
