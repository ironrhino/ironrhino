package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.security.verfication.WrongVerificationCodeException;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.security.DefaultWebAuthenticationDetails;
import org.ironrhino.core.spring.security.password.PasswordCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component
public class VerificationPasswordCheckInterceptor implements PasswordCheckInterceptor {

	@Autowired
	private VerificationManager verificationManager;

	@Value("${verification.code.qualified:true}")
	private boolean verificationCodeQualified = true;

	@Override
	public void prePasswordCheck(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
		if (verificationManager.isVerificationRequired(userDetails)) {
			String verificationCode = ((DefaultWebAuthenticationDetails) authentication.getDetails())
					.getVerificationCode();
			if (verificationCode == null && skipPasswordCheck(userDetails))
				// use password parameter instead for exchange access token
				verificationCode = String.valueOf(authentication.getCredentials());
			boolean verified = verificationManager.verify(userDetails, verificationCode);
			if (!verified)
				throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
		}
	}

	@Override
	public boolean skipPasswordCheck(UserDetails userDetails) {
		return verificationCodeQualified && verificationManager.isVerificationRequired(userDetails)
				&& !verificationManager.isPasswordRequired(userDetails);
	}

}
