package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordGenerator {

	String generate(UserDetails user);

}
