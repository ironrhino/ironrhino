package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface ConcreteUserDetailsService {

	public default boolean accepts(String username) {
		return true;
	}

	public UserDetails loadUserByUsername(String username);

}
