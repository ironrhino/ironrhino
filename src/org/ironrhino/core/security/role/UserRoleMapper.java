package org.ironrhino.core.security.role;

import org.springframework.security.core.userdetails.UserDetails;

@FunctionalInterface
public interface UserRoleMapper {

	String[] map(UserDetails user);

}
