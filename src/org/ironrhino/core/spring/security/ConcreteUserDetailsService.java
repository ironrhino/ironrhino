package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface ConcreteUserDetailsService<T extends UserDetails> {

	default boolean accepts(String username) {
		return true;
	}

	T loadUserByUsername(String username);

	default T updatePassword(UserDetails user, String encodedPassword) {
		throw new UnsupportedOperationException();
	}

}
