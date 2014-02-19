package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Owner {

	String propertyName() default "createUser";

	boolean isolate() default true;

	boolean readonlyForOther() default true;

	String supervisorRole() default "";

}