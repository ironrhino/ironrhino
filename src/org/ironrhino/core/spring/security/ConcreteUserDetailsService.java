package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface ConcreteUserDetailsService {

	public boolean accepts(String username);

	public UserDetails loadUserByUsername(String username);

}
