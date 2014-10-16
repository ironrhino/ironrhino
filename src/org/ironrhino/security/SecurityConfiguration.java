package org.ironrhino.security;

import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.security.model.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SecurityConfiguration {

	@Bean
	@LoggedInUser
	@Scope("prototype")
	public User loggedInUser() {
		return AuthzUtils.getUserDetails(User.class);
	}

}