package org.ironrhino.core.cache;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(METHOD)
@Retention(RUNTIME)
public @interface CheckCache {
	// mvel expression
	String key();

	// mvel expression
	String namespace() default "";

	// mvel expression
	String when() default "true";

	// mvel expression
	String timeToLive() default "3600";

	// mvel expression
	String timeToIdle() default "-1";

	TimeUnit timeUnit() default TimeUnit.SECONDS;

	boolean eternal() default false;

	boolean cacheNull() default false;

	int throughPermits() default 5;

	// TimeUnit.MILLISECONDS
	int waitTimeout() default 200;

	// mvel expression
	String onHit() default "";

	// mvel expression
	String onMiss() default "";

	// mvel expression
	String onPut() default "";

}
