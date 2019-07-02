package org.ironrhino.core.security.role;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserRoleMapper {

	String[] map(UserDetails user);

}
