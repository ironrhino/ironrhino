package org.ironrhino.core.exception;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface ExceptionCreator {

	String project();
	
	String module();

	int length() default 4;

	Class<? extends Exception> type() default BusinessException.class;

}
