package org.ironrhino.core.security.role;

public interface UserRoleFilter {

	public boolean accepts(String username, String role);

}
