package org.ironrhino.core.spring.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户扩展接口定义
 */
public interface ConcreteUserDetailsService {

	public boolean accepts(String username);

	public UserDetails loadUserByUsername(String username);

}
