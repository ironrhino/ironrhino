package org.ironrhino.core.spring.security.password;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordCheckInterceptor {

	default void prePasswordCheck(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {

	}

	default void postPasswordCheck(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {

	}

	default boolean skipPasswordCheck(UserDetails userDetails) {
		return false;
	}

}
