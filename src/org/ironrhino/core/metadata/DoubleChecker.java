package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.ValueGenerationType;
import org.ironrhino.core.hibernate.DoubleCheckerGeneration;

@ValueGenerationType(generatedBy = DoubleCheckerGeneration.class)
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface DoubleChecker {

	static final String PARAMETER_NAME_USERNAME = "doubleCheckUsername";

	static final String PARAMETER_NAME_PASSWORD = "doubleCheckPassword";

	String value();

}
