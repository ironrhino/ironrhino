package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface RemotingUserDetailsService<T extends UserDetails> {

	default boolean accepts(String username) {
		return true;
	}

	T loadUserByUsername(String username);

}
