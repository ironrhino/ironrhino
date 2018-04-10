package org.ironrhino.core.search.elasticsearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Searchable {

	String type() default "";

	float boost() default 1.0f;

	boolean root() default true;

	String analyzer() default "";

	String converter() default "";
}