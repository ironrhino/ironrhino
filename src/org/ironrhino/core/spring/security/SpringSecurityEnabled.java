package org.ironrhino.core.spring.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public @interface SpringSecurityEnabled {

}
