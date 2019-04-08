package pointdb.process;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Tag.List.class)
//@Inherited // Tag is collected at super class and outer class directly
public @interface Tag {
	String[] value();
	
	@Retention(RUNTIME)
	@Target(TYPE) 
	@interface List {
		Tag[] value();
	}
}

