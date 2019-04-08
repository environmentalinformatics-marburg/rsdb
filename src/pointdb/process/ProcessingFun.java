package pointdb.process;

import java.util.Arrays;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ProcessingFun {
	private static final Logger log = LogManager.getLogger();

	public final String name;
	public final String description;
	public final String[] tags;

	public ProcessingFun() {
		Class<? extends ProcessingFun> clazz = this.getClass();
		String className = clazz.getSimpleName();
		if(className.startsWith("Fun_")) {
			className = className.substring(4);
		}
		this.name = className;
		Description descriptionAnnotation = clazz.getAnnotation(Description.class);
		if(descriptionAnnotation == null) {
			log.info(name + " no description");
		} else {			
			//log.info(name + ": "+descriptionAnnotation.value());
		}
		this.description = descriptionAnnotation == null ? name : descriptionAnnotation.value();

		this.tags = collectTags(clazz);
	}

	private static String[] collectTags(Class<? extends ProcessingFun> clazz) {
		TreeSet<String> set = new TreeSet<>();
		collectTags(clazz, set);
		return set.toArray(new String[0]);
	}

	private static void collectTags(Class<?> clazz, TreeSet<String> set) {
		Tag[] tagAnnotations = clazz.getAnnotationsByType(Tag.class);
		for(Tag tagAnnotation:tagAnnotations) {
			set.addAll(Arrays.asList(tagAnnotation.value()));
			//log.info(clazz.getName()+" tag "+Arrays.toString(tagAnnotation.value()));
		}
		Class<?> superClazz = clazz.getSuperclass();
		if(superClazz != null && superClazz != Object.class) {
			collectTags(superClazz, set);
		}
		Class<?> enclosingClazz = clazz.getEnclosingClass();
		if(enclosingClazz != null) {
			collectTags(enclosingClazz, set);
		}
	}
	
	public ProcessingFun(String name, String description) {
		this.name = name;
		this.description = description;
		this.tags = collectTags(this.getClass());
	}

	public ProcessingFun(String name, String description, String[] tags) {
		this.name = name;
		this.description = description;
		this.tags = tags;
	}

	public double process(DataProvider2 provider) {
		return Double.MIN_NORMAL;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}