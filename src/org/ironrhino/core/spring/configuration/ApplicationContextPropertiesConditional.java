package org.ironrhino.core.spring.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(ApplicationContextPropertiesCondition.class)
@Repeatable(ApplicationContextPropertiesConditionals.class)
public @interface ApplicationContextPropertiesConditional {

	public static final String ANY = "^_^";

	String key();

	String value();

	boolean negated() default false;

}
