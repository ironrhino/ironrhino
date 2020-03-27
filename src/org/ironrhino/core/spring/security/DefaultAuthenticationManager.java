package org.ironrhino.core.spring.security;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.Getter;

public class DefaultAuthenticationManager extends ProviderManager {

	private static final String CACHE_NAMESPACE = "fla"; // Failed Login Attempts

	@Autowired
	private CacheManager cacheManager;

	@Value("${authenticationManager.lockdownForMinutes:60}") // 1 hour
	private int lockdownForMinutes = 60;

	@Value("${authenticationManager.maxAttempts:5}")
	private int maxAttempts = 5;

	@Value("${authenticationManager.usernameMaxLength:32}")
	private int usernameMaxLength = 32;

	@Autowired
	@Getter
	private List<AuthenticationProvider> providers;

	public DefaultAuthenticationManager() {
		// use dummy AuthenticationProvider to avoid exception throws by super class
		super(Collections.singletonList(new AuthenticationProvider() {
			@Override
			public Authentication authenticate(Authentication auth) throws AuthenticationException {
				return null;
			}

			@Override
			public boolean supports(Class<?> paramClass) {
				return false;
			}
		}));
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		if (StringUtils.isBlank(username))
			return super.authenticate(authentication);
		if (username.length() > usernameMaxLength)
			throw new UsernameNotFoundException("Bad username");
		long times = cacheManager.increment(username, 0, 0, TimeUnit.SECONDS, CACHE_NAMESPACE);
		if (times >= maxAttempts)
			throw new LockedException("Failed login attempts exceed " + maxAttempts);
		try {
			Authentication auth = super.authenticate(authentication);
			cacheManager.delete(username, CACHE_NAMESPACE);
			return auth;
		} catch (CredentialsExpiredException e) {
			cacheManager.delete(username, CACHE_NAMESPACE);
			throw e;
		} catch (BadCredentialsException e) {
			cacheManager.increment(username, 1, lockdownForMinutes, TimeUnit.MINUTES, CACHE_NAMESPACE);
			throw e;
		}
	}

}
