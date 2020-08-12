package org.ironrhino.core.security.role;

import java.util.Map;

@FunctionalInterface
public interface UserRoleProvider {

	Map<String, String> getRoles();

}
