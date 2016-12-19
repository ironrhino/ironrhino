package org.ironrhino.rest.doc.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface Status {

	int code();

	String message() default "";

	String description() default "";

}