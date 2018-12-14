package org.ironrhino.core.spring.security;

import java.util.Collections;
import java.util.List;

import org.ironrhino.core.spring.security.password.PasswordCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class DefaultDaoAuthenticationProvider extends DaoAuthenticationProvider {

	@Autowired(required = false)
	private List<PasswordCheckInterceptor> passwordCheckInterceptors = Collections.emptyList();

	public DefaultDaoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		setUserDetailsService(userDetailsService);
		setPasswordEncoder(passwordEncoder);
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
		for (PasswordCheckInterceptor interceptor : passwordCheckInterceptors)
			interceptor.prePasswordCheck(userDetails, authentication);
		// any checker skip then skip password check
		if (passwordCheckInterceptors.stream().noneMatch(interceptor -> interceptor.skipPasswordCheck(userDetails)))
			super.additionalAuthenticationChecks(userDetails, authentication);
		for (PasswordCheckInterceptor interceptor : passwordCheckInterceptors)
			interceptor.postPasswordCheck(userDetails, authentication);
	}

}
