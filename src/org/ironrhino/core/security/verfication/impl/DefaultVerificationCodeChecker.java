package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerificationCodeEnabled;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.spring.security.VerificationCodeChecker;
import org.ironrhino.core.spring.security.WrongVerificationCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@VerificationCodeEnabled
@Component
public class DefaultVerificationCodeChecker implements VerificationCodeChecker {

	@Autowired
	private VerificationManager verificationManager;

	@Value("${verification.code.qualified:true}")
	private boolean verificationCodeQualified = true;

	@Override
	public boolean skip(String username) {
		return !verificationManager.isVerificationRequired(username);
	}

	@Override
	public boolean skip(UserDetails userDetails) {
		return !verificationManager.isVerificationRequired(userDetails);
	}

	@Override
	public boolean skipSend() {
		return false;
	}

	@Override
	public void verify(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication,
			String verificationCode) {
		if (verificationCode == null && skipPasswordCheck(userDetails))
			// use password parameter instead for exchange access token
			verificationCode = String.valueOf(authentication.getCredentials());
		if (!verificationManager.verify(userDetails, verificationCode))
			throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
	}

	@Override
	public boolean skipPasswordCheck(UserDetails userDetails) {
		return verificationCodeQualified && verificationManager.isVerificationRequired(userDetails)
				&& !verificationManager.isPasswordRequired(userDetails);
	}

	@Override
	public boolean skipPasswordCheck(String username) {
		return verificationCodeQualified && verificationManager.isVerificationRequired(username)
				&& !verificationManager.isPasswordRequired(username);
	}

}
