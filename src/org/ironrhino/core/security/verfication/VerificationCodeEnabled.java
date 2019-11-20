package org.ironrhino.core.security.verfication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
public @interface VerificationCodeEnabled {

}
