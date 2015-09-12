package org.ironrhino.rest.doc.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD, PARAMETER })
@Retention(RUNTIME)
public @interface Fields {

	Field[]value() default {};

	String sample() default "";

	String sampleMethodName() default "";

	String sampleFileName() default "";

}