package org.ironrhino.core.spring.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(ServiceImplementationCondition.class)
public @interface ServiceImplementationConditional {

	String[]profiles() default {};

	Class<?>serviceInterface() default void.class;

}
