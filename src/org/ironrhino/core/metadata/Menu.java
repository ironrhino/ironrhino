package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD, TYPE })
@Retention(RUNTIME)
public @interface Menu {

	String name();

	int displayOrder() default 0;

	String[] parents() default {};

}
