package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordStrengthChecker {

	public void check(UserDetails user, String password);

}
