package org.ironrhino.core.spring.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(BeanPresentCondition.class)
public @interface BeanPresentConditional {

	String value() default ""; // alias for name

	String name() default "";

	Class<?> type() default void.class;

	boolean negated() default false;

}