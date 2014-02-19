package org.ironrhino.core.aop;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.ironrhino.core.metadata.Scope;

@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface PublishAware {
	Scope scope() default Scope.APPLICATION;
}
