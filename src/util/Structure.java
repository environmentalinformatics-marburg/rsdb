package util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;

import org.mapdb.Serializer;

public class Structure<T>{

	private static final Class<?> NO_CLASS = new Object(){}.getClass();

	Class<T> clazz;
	Comparator<T> comparator;
	Serializer<T> serializer;

	public Structure() {		
		this.clazz = getTypeClass(this);
	}

	public Structure(Class<T> clazz) {
		if(clazz!=null) {
			this.clazz = clazz;
		} else {
			this.clazz = getTypeClass(this);
		}
	}

	public Structure(T exampleObject) {		
		if(clazz!=null) {
			this.clazz = (Class<T>) exampleObject.getClass();
		} else {
			this.clazz = getTypeClass(this);
		}		
	}
	
	private static <T> Class<T> getTypeClass(Object o) {
		Type superClass = o.getClass().getGenericSuperclass();		
		if(superClass instanceof ParameterizedType) {
			return ((Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0]);	
		} else {
			return (Class<T>) NO_CLASS;  
		}
	}

	public void setComparator(Comparator<T> comparator) {
		this.comparator = comparator;
		if(this.clazz==NO_CLASS) {
			this.clazz = getTypeClass(comparator);
		}
	}

	public void setSerializer(Serializer<T> serializer) {
		this.serializer = serializer;
		if(this.clazz==NO_CLASS) {
			this.clazz = getTypeClass(serializer);
		}
	}

	public Class<T> getClazz() {
		return clazz;
	}

	public Comparator<T> getComparator() {
		return comparator;
	}

	public Serializer<T> getSerializer() {
		return serializer;
	}
}
