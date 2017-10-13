package org.ironrhino.rest.client;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Retention(RUNTIME)
@Target(TYPE)
public @interface RestApi {

	@AliasFor("name")
	String value() default "";

	@AliasFor("value")
	String name() default "";

	String restTemplate() default "";

	String restClient() default "";

	String apiBaseUrl() default "";

}
