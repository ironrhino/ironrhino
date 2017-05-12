package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(RUNTIME)
public @interface DoubleCheck {

	static final String ATTRIBUTE_NAME_DOUBLE_CHECKER = "_DOUBLE_CHECKER";

	static final String PARAMETER_NAME_USERNAME = "doubleCheckUsername";

	static final String PARAMETER_NAME_PASSWORD = "doubleCheckPassword";

	String value();

}
