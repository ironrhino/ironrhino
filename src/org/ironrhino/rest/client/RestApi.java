package org.ironrhino.rest.client;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface RestApi {

	String value() default "";

	String restTemplate() default "";

	String restClient() default "";

	String apiBaseUrl() default "";

}
