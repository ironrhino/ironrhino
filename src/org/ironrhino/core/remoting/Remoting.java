package org.ironrhino.core.remoting;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.validation.annotation.Validated;

@Validated
@Target(TYPE)
@Retention(RUNTIME)
public @interface Remoting {

	@AliasFor("serviceInterfaces")
	Class<?>[] value() default {};

	@AliasFor("value")
	Class<?>[] serviceInterfaces() default {};

	String description() default "";

}
