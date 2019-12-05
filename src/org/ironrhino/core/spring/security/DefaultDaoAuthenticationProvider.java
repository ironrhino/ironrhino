package org.ironrhino.core.spring.security;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component("daoAuthenticationProvider")
@Order(0)
@SpringSecurityEnabled
public class DefaultDaoAuthenticationProvider extends DaoAuthenticationProvider {

	@Autowired(required = false)
	private List<VerificationCodeChecker> verificationCodeCheckers = Collections.emptyList();

	public DefaultDaoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
			UserDetailsPasswordService userDetailsPasswordService) {
		setUserDetailsService(userDetailsService);
		setPasswordEncoder(passwordEncoder);
		setUserDetailsPasswordService(userDetailsPasswordService);
	}

	@Override
	public UserDetailsChecker getPreAuthenticationChecks() {
		return super.getPreAuthenticationChecks();
	}

	@Override
	public UserDetailsChecker getPostAuthenticationChecks() {
		return super.getPostAuthenticationChecks();
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
		String verificationCode = ((DefaultWebAuthenticationDetails) authentication.getDetails()).getVerificationCode();
		boolean skipPasswordCheck = false;
		AuthenticationException ex = null;
		for (VerificationCodeChecker checker : verificationCodeCheckers) {
			if (!checker.skip(userDetails)) {
				try {
					checker.verify(userDetails, authentication, verificationCode);
					if (checker.skipPasswordCheck(userDetails))
						skipPasswordCheck = true;
					ex = null;
					break;
				} catch (AuthenticationException e) {
					ex = e;
					continue;
				}
			}
		}
		if (ex != null)
			throw ex;
		if (!skipPasswordCheck)
			super.additionalAuthenticationChecks(userDetails, authentication);
	}

}
