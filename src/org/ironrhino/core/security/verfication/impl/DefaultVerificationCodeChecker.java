package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.security.VerificationCodeChecker;
import org.ironrhino.core.spring.security.WrongVerificationCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component
public class DefaultVerificationCodeChecker implements VerificationCodeChecker {

	@Autowired
	private VerificationManager verificationManager;

	@Value("${verification.code.qualified:true}")
	private boolean verificationCodeQualified = true;

	@Override
	public void verify(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication,
			String verificationCode) {
		if (verificationManager.isVerificationRequired(userDetails)) {
			if (verificationCode == null && skipPasswordCheck(userDetails))
				// use password parameter instead for exchange access token
				verificationCode = String.valueOf(authentication.getCredentials());
			if (!verificationManager.verify(userDetails, verificationCode))
				throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
		}
	}

	@Override
	public boolean skipPasswordCheck(UserDetails userDetails) {
		return verificationCodeQualified && verificationManager.isVerificationRequired(userDetails)
				&& !verificationManager.isPasswordRequired(userDetails);
	}

}
