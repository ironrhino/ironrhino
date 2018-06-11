package org.ironrhino.core.throttle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface CircuitBreaker {

	float failureRateThreshold() default 50;

	int waitDurationInOpenState() default 60;

	int ringBufferSizeInHalfOpenState() default 10;

	int ringBufferSizeInClosedState() default 100;

	Class<? extends Throwable>[] include();

	Class<? extends Throwable>[] exclude() default {};

}
