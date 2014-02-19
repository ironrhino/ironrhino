package org.ironrhino.core.aop;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface RetryConfig {

	Class<?>[] recoverableExceptions() default { IOException.class };

	int maxTimes() default 3;

	long incrementalFactor() default 10L;

	long interval() default 50L;
}
