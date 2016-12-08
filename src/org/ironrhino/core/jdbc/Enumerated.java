package org.ironrhino.core.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.EnumType;

@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Enumerated {
	EnumType value() default EnumType.ORDINAL;
}