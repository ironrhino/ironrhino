package org.ironrhino.core.throttle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface Frequency {

	/**
	 * 
	 * this attribute support expression
	 * 
	 * @return
	 */
	String limits();

	int duration() default 1;

	TimeUnit timeUnit() default TimeUnit.HOURS;

}
