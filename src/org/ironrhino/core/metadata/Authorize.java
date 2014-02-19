package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.ironrhino.core.security.dynauth.DynamicAuthorizer;

@Target({ METHOD, TYPE })
@Retention(RUNTIME)
public @interface Authorize {
	// equals to tag security:authorize
	String ifAllGranted() default "";

	String ifAnyGranted() default "";

	String ifNotGranted() default "";

	Class<? extends DynamicAuthorizer> authorizer() default DynamicAuthorizer.class;

	String resourceGroup() default "";

}
