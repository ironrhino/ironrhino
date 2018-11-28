package org.ironrhino.core.tracing;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ PARAMETER, FIELD })
@Retention(RUNTIME)
public @interface Tag {

	String name();

	String value();

}