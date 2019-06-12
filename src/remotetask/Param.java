package remotetask;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Param.List.class)
public @interface Param {
	String name();
	String type() default "string";
	String desc() default "";
	boolean required() default true;
	
	@Retention(RUNTIME)
	@Target(TYPE) 
	@interface List {
		Param[] value();
	}
}
