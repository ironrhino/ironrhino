package org.ironrhino.core.security.role;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserRoleMapper {

	public String[] map(UserDetails user);

}
