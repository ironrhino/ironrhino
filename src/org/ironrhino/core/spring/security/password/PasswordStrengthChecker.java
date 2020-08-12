package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

@FunctionalInterface
public interface PasswordStrengthChecker {

	void check(UserDetails user, String password);

}
