package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface ConcreteUserDetailsService<T extends UserDetails> {

	public default boolean accepts(String username) {
		return true;
	}

	public T loadUserByUsername(String username);

}
