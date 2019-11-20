package org.ironrhino.core.spring.security;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.stereotype.Component;

@Component("rememberMeAuthenticationProvider")
@Order(Ordered.LOWEST_PRECEDENCE)
@ApplicationContextPropertiesConditional(key = "rememberMe.disabled", value = "true", negated = true)
@SpringSecurityEnabled
public class DefaultRememberMeAuthenticationProvider extends RememberMeAuthenticationProvider {

	public DefaultRememberMeAuthenticationProvider(@Value("${rememberMe.key:youcannotguessme}") String key) {
		super(key);
	}

}
