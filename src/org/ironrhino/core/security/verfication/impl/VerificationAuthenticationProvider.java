package org.ironrhino.core.security.verfication.impl;

import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.security.verfication.WrongVerificationCodeException;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.security.DefaultWebAuthenticationDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component
public class VerificationAuthenticationProvider extends DaoAuthenticationProvider {

	@Autowired
	private VerificationManager verificationManager;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Value("${verification.code.qualified:true}")
	private boolean verificationCodeQualified = true;

	public VerificationAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		setUserDetailsService(userDetailsService);
		setPasswordEncoder(passwordEncoder);
	}

	@PostConstruct
	public void init() {
		List<AuthenticationProvider> providers = ((ProviderManager) authenticationManager).getProviders();
		Iterator<AuthenticationProvider> it = providers.iterator();
		while (it.hasNext()) {
			if (it.next() instanceof DaoAuthenticationProvider) {
				it.remove();
				break;
			}
		}
		providers.add(0, this);
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
		if (verificationManager.isVerificationRequired(userDetails)) {
			String verificationCode = ((DefaultWebAuthenticationDetails) authentication.getDetails())
					.getVerificationCode();
			if (verificationCode == null) // use password parameter instead for exchange access token
				verificationCode = String.valueOf(authentication.getCredentials());
			boolean verified = verificationManager.verify(userDetails, verificationCode);
			if (!verified)
				throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
			if (verificationCodeQualified && !verificationManager.isPasswordRequired(userDetails))
				return; // skip check password
		}
		super.additionalAuthenticationChecks(userDetails, authentication);
	}

}
