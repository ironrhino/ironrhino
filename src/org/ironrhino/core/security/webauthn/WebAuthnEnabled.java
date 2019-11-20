package org.ironrhino.core.security.webauthn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ApplicationContextPropertiesConditional(key = "webAuthn.enabled", value = "true")
public @interface WebAuthnEnabled {

}
