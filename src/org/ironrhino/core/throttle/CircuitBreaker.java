package org.ironrhino.core.throttle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface CircuitBreaker {

	float failureRateThreshold() default 95;

	float slowCallRateThreshold() default 100;

	int slowCallDurationThreshold() default 60; // Seconds

	int waitDurationInOpenState() default 60; // Seconds

	int permittedNumberOfCallsInHalfOpenState() default 10;

	int minimumNumberOfCalls() default 100;

	int slidingWindowSize() default 100;

	Class<? extends Throwable>[] include();

	Class<? extends Throwable>[] exclude() default {};

}
