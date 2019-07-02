package org.ironrhino.core.security.role;

import java.util.Collection;

import org.springframework.security.core.userdetails.UserDetails;

public interface RoledUserDetails extends UserDetails {

	Collection<String> getRoles();
}
