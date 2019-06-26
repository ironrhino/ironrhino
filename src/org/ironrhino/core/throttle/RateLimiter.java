package org.ironrhino.core.throttle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface RateLimiter {

	long timeoutDuration() default 5000; // ms

	long limitRefreshPeriod() default 500; // ms

	int limitForPeriod() default 100;

}
