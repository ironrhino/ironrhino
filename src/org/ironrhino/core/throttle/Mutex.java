package org.ironrhino.core.throttle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.ironrhino.core.metadata.Scope;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface Mutex {

	String key() default "";

	Scope scope() default Scope.GLOBAL;

}
