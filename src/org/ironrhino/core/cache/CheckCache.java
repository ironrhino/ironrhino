package org.ironrhino.core.cache;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(METHOD)
@Retention(RUNTIME)
public @interface CheckCache {
	// mvel or spel expression
	String key();

	// mvel or spel expression
	String namespace() default "";

	// mvel or spel expression
	String when() default "true";

	// mvel or spel expression
	String timeToLive() default "3600";

	// mvel or spel expression
	String timeToIdle() default "-1";

	TimeUnit timeUnit() default TimeUnit.SECONDS;

	boolean eternal() default false;

	boolean cacheNull() default false;

	int throughPermits() default 5;

	// TimeUnit.MILLISECONDS
	int waitTimeout() default 200;

	// mvel or spel expression
	String onHit() default "";

	// mvel or spel expression
	String onMiss() default "";

	// mvel or spel expression
	String onPut() default "";

}
