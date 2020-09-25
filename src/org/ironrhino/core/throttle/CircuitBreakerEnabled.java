package org.ironrhino.core.throttle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.spring.configuration.StageConditional;
import org.ironrhino.core.util.AppInfo.Stage;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@StageConditional(Stage.PRODUCTION)
@ClassPresentConditional("io.github.resilience4j.circuitbreaker.CircuitBreaker")
public @interface CircuitBreakerEnabled {

}
