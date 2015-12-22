package org.ironrhino.core.throttle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface Concurrency {

	String key() default "";

	String permits();

	boolean block() default false;

	int timeout() default 0;

	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
