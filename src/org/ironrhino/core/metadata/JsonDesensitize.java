package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * properties are ignored in JsonDesensitizer.toJson()
 * 
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface JsonDesensitize {

	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

	String value() default DEFAULT_NONE;

}
