package org.ironrhino.core.security.dynauth;

import org.springframework.security.core.userdetails.UserDetails;

public interface DynamicAuthorizer {

	boolean authorize(UserDetails user, String resource);

}