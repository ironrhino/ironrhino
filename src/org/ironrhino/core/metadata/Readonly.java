package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(RUNTIME)
public @interface Readonly {

	boolean value() default false; // all readonly

	String expression() default ""; // some entity readonly

	boolean deletable() default false; // can delete when readonly
}