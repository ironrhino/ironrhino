package org.ironrhino.core.security.dynauth;

import org.springframework.security.core.userdetails.UserDetails;

public interface DynamicAuthorizer {

	public boolean authorize(UserDetails user, String resource);

}