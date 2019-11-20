package org.ironrhino.core.security.otp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ApplicationContextPropertiesConditional(key = "totp.enabled", value = "true")
public @interface TotpEnabled {

}
