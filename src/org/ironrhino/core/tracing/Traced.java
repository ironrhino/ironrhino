package org.ironrhino.core.tracing;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface Traced {

	boolean withActiveSpanOnly() default true;

	String operationName() default "";

	Tag[] tags() default {};

}
