package pointdb.process;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.TreeMap;
import java.util.TreeSet;


import org.tinylog.Logger;
import pointdb.process.Fun_BE.Fun_BE_PR_INTERVAL;
import pointdb.process.Fun_BE.Fun_BE_PR_H;
import pointdb.process.Fun_BE.Fun_BE_PR_Q;
import pointdb.process.Fun_BE.Fun_BE_RD_INTERVAL;
import pointdb.process.Fun_BE.Fun_BE_RD_H;
import pointdb.process.Fun_BE.Fun_BE_RD_Q;

public class Functions {
	

	private static final TreeMap<String, ProcessingFun> funMap = new TreeMap<>();
	private static final TreeSet<String> funTags = new TreeSet<>();
	
	static {
		add(Fun_point_count.class);
		add(Fun_ground_point_count.class);
		add(Fun_vegetation_point_count.class);
		add(Fun_bbox_area.class);
		add(Fun_area.class);
		add(Fun_point_density.class);
		add(Fun_ground_point_density.class);
		add(Fun_vegetation_point_density.class);
		add(Fun_point_coverage.class);
		//add(Fun_canopy_height.class);
		add(Fun_LAI.class);
		add(Fun_VDR.class);

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
		addContained(Fun_surface_area.class);
		addContained(Fun_forest_structure.class);
		addContained(Fun_ENL.class);
		
		for (int i = 1; i <= 55; i++) {
        	Functions.add(new Fun_BE_PR_INTERVAL(i));			
		}
		
		for (int i = 1; i <= 10; i++) {
        	Functions.add(new Fun_BE_PR_H(i));			
		}
		for (int i = 1; i <= 4; i++) {
			Functions.add(new Fun_BE_PR_Q(i));			
		}
		
		int LAYER_MAX = 55;
		for (int i = 1; i <= LAYER_MAX; i++) {
			double hsetMin = i == 1 ? Double.NEGATIVE_INFINITY : i - 1;
			double hsetMax = i == LAYER_MAX ? Double.POSITIVE_INFINITY : i;
			String name = "BE_RD_" + (i<=9 ? "0" : "") + i;
			String description = "Return density of " + i + " meter layer (based on point height above ground)";
			if(i == 1) {
				description = "Return density of " + i + " meter layer (points below included) (based on point height above ground)";
			}
			if( i == LAYER_MAX) {
				description = "Return density of " + i + " meter layer (points above included) (based on point height above ground)";
			}
        	Functions.add(new Fun_BE_RD_INTERVAL(hsetMin, hsetMax, name, description));			
		}
		
		for (int i = 1; i <= 10; i++) {
        	Functions.add(new Fun_BE_RD_H(i));			
		}
		for (int i = 1; i <= 4; i++) {
			Functions.add(new Fun_BE_RD_Q(i));			
		}
		
		for(ProcessingFun fun:funMap.values()) {
			for(String tag:fun.tags) {
				funTags.add(tag);
			}
		}
	}

	public static void add(ProcessingFun fun) {
		if(funMap.containsKey(fun.name)) {
			Logger.warn("overwriting existing function "+fun.name);
		}
		funMap.put(fun.name, fun);
	}

	@SuppressWarnings("unchecked")
	private static void addContained(Class<?> overClass) {
		for(Class<?> clazz:overClass.getDeclaredClasses()) {
			if(ProcessingFun.class.isAssignableFrom(clazz)) {
				add((Class<? extends ProcessingFun>) clazz);
			} else {
				Logger.warn("class not compatible to ProcessingFun: "+clazz);
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
					add(clazz.getDeclaredConstructor().newInstance());
				} catch (Exception e) {
					Logger.error(e);
				}
			} else {
				Logger.error("class does not have empty constructor: "+clazz);
			}
			} else {
				Logger.error("inner class is not static: "+clazz);
			}
		} else {
			Logger.info("class excluded by marker: "+clazz);
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
