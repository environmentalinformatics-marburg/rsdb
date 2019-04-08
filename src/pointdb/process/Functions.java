package pointdb.process;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pointdb.process.Fun_BE.Fun_BE_PR_INTERVAL;
import pointdb.process.Fun_BE.Fun_BE_RD_INTERVAL;

public class Functions {
	static final Logger log = LogManager.getLogger();

	private static final TreeMap<String, ProcessingFun> funMap = new TreeMap<>();
	private static final TreeSet<String> funTags = new TreeSet<>();
	
	static {
		add(Fun_point_count.class);
		add(Fun_bbox_area.class);
		add(Fun_area.class);
		add(Fun_point_density.class);
		add(Fun_point_coverage.class);
		//add(Fun_canopy_height.class);
		add(Fun_LAI.class);

		addContained(Fun_BE.class);
		addContained(Fun_pulse.class);
		addContained(Fun_dtm_aspect.class);
		addContained(Fun_dtm_slope.class);
		addContained(Fun_scan_angle.class);
		addContained(Fun_chm_height.class);
		addContained(Fun_surface_intensity.class);
		addContained(Fun_dsm_elevation.class);
		addContained(Fun_dtm_elevation.class);
		addContained(Fun_point_elevation.class);
		addContained(Fun_vegetation_coverage.class);
		addContained(Fun_surface_ratio.class);
		addContained(Fun_forest_structure.class);
		
		for (int i = 1; i <= 55; i++) {
        	Functions.add(new Fun_BE_PR_INTERVAL(i));			
		}
		for (int i = 1; i <= 55; i++) {
        	Functions.add(new Fun_BE_RD_INTERVAL(i));			
		}
		
		for(ProcessingFun fun:funMap.values()) {
			for(String tag:fun.tags) {
				funTags.add(tag);
			}
		}
	}

	public static void add(ProcessingFun fun) {
		if(funMap.containsKey(fun.name)) {
			log.warn("overwriting existing function "+fun.name);
		}
		funMap.put(fun.name, fun);
	}

	private static void addContained(Class<?> overClass) {
		for(Class<?> clazz:overClass.getDeclaredClasses()) {
			if(ProcessingFun.class.isAssignableFrom(clazz)) {
				add((Class<? extends ProcessingFun>) clazz);
			} else {
				log.warn("class not compatible to ProcessingFun: "+clazz);
			}
		}

	}

	private static boolean hasEmptyConstructor(Class<?> clazz) {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		for(Constructor<?> constructor:constructors) {
			if(constructor.getParameterCount()==0) {
				return true;
			}
		}
		return false;
	}
	
	private static void add(Class<? extends ProcessingFun> clazz) {		
		if(clazz.getAnnotation(Exculde.class) == null) {
			if((!clazz.isMemberClass()) || Modifier.isStatic(clazz.getModifiers())) {
			if(hasEmptyConstructor(clazz)) {
				try {
					add(clazz.newInstance());
				} catch (Exception e) {
					log.error(e);
				}
			} else {
				log.error("class does not have empty constructor: "+clazz);
			}
			} else {
				log.error("inner class is not static: "+clazz);
			}
		} else {
			log.info("class excluded by marker: "+clazz);
		}
	}

	public static double apply(DataProvider2 provider, String func) {
		ProcessingFun fun = funMap.get(func);
		if(fun==null) {
			new RuntimeException("unknown function "+func);
		}
		return fun.process(provider);
	}
	
	public static ProcessingFun getFun(String func) {
		ProcessingFun fun = funMap.get(func);
		if(fun==null) {
			throw new RuntimeException("unknown function |" + func + "|");
		}
		return fun;
	}

	public static ProcessingFun[] getFunctions() {
		return funMap.values().toArray(new ProcessingFun[0]);
	}
	
	public static String[] getTags() {
		return funTags.toArray(new String[0]);
	}
}
