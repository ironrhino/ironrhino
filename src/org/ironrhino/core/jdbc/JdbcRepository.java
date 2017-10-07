package org.ironrhino.core.jdbc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface JdbcRepository {

	String value() default "";

	String dataSource() default "dataSource";

	String jdbcTemplate() default "";

}
