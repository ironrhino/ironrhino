package org.ironrhino.security;

import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.security.model.User;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

@Order(0)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class SecurityConfiguration {

	@Bean
	@LoggedInUser
	@Scope("prototype")
	public User loggedInUser() {
		return AuthzUtils.getUserDetails(User.class);
	}

}