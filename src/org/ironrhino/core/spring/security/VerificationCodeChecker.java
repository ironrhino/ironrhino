package org.ironrhino.core.spring.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public interface VerificationCodeChecker {

	void verify(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication, String verificationCode);

	default boolean skipPasswordCheck(UserDetails userDetails) {
		return false;
	}

}
