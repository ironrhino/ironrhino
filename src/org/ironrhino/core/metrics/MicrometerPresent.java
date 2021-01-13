package org.ironrhino.core.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ClassPresentConditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ClassPresentConditional("io.micrometer.core.instrument.Metrics")
public @interface MicrometerPresent {

}
