package org.ironrhino.core.security.role;

import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserRoleFilter {

	public Map<String, String> filter(UserDetails user, Map<String, String> roles);

}
