package org.ironrhino.core.jdbc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Retention(RUNTIME)
@Target(TYPE)
public @interface JdbcRepository {

	@AliasFor("name")
	String value() default "";

	@AliasFor("value")
	String name() default "";

	String dataSource() default "dataSource";

	String jdbcTemplate() default "";

}
