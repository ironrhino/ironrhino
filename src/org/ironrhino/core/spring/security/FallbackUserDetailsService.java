package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface FallbackUserDetailsService extends UserDetailsService {
	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException;
}
