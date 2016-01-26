package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface RemotingUserDetailsService {

	public UserDetails loadUserByUsername(String username);

}
