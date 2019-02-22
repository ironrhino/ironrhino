package org.ironrhino.core.spring.security;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class DefaultAuthenticationManager extends ProviderManager {

	private static final String CACHE_NAMESPACE = "fla"; // Failed Login Attempts

	@Autowired
	private CacheManager cacheManager;

	@Value("${authenticationManager.lockdownForMinutes:60}") // 1 hour
	private int lockdownForMinutes = 60;

	@Value("${authenticationManager.maxAttempts:5}")
	private int maxAttempts = 5;

	public DefaultAuthenticationManager(List<AuthenticationProvider> providers) {
		super(providers);
	}

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		if (StringUtils.isBlank(username))
			return super.authenticate(authentication);
		long times = cacheManager.increment(username, 0, 0, TimeUnit.SECONDS, CACHE_NAMESPACE);
		if (times >= maxAttempts)
			throw new LockedException("Failed login attempts exceed " + maxAttempts);
		try {
			Authentication auth = super.authenticate(authentication);
			cacheManager.delete(username, CACHE_NAMESPACE);
			return auth;
		} catch (BadCredentialsException e) {
			cacheManager.increment(username, 1, lockdownForMinutes, TimeUnit.MINUTES, CACHE_NAMESPACE);
			throw e;
		}
	}

}
