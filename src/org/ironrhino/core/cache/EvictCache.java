package org.ironrhino.core.cache;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(RUNTIME)
public @interface EvictCache {

	// mvel or spel expression
	String key();

	// mvel or spel expression
	String namespace() default "";

	// mvel or spel expression
	String onEvict() default "";

	// mvel or spel expression
	String renew() default "";

	// mvel or spel expression
	String renewTimeToLive() default "3600";

}
