package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordStrengthChecker {

	void check(UserDetails user, String password);

}
