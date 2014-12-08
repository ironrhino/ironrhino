package org.ironrhino.rest.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Field {

	String name() default "";

	String type() default "";

	boolean required() default false;

	String label() default "";

	String description() default "";

	String defaultValue() default "";

}