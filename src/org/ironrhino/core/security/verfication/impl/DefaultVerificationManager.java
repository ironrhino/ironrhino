package org.ironrhino.core.security.verfication.impl;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.verfication.ReceiverNotFoundException;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.security.verfication.VerificationService;
import org.ironrhino.core.security.verfication.WrongVerificationCodeException;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component("verificationManager")
public class DefaultVerificationManager implements VerificationManager {

	@Autowired
	private VerificationService verificationService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Override
	public boolean isVerificationRequired(String username) {
		try {
			return isVerificationRequired(userDetailsService.loadUserByUsername(username));
		} catch (UsernameNotFoundException e) {
			return false;
		}
	}

	@Override
	public boolean isPasswordRequired(String username) {
		try {
			return isPasswordRequired(userDetailsService.loadUserByUsername(username));
		} catch (UsernameNotFoundException e) {
			return true;
		}
	}

	@Override
	public void send(String username) throws ReceiverNotFoundException {
		verificationService.send(getReceiver(userDetailsService.loadUserByUsername(username)));
	}

	@Override
	public void verify(UserDetails user) throws WrongVerificationCodeException {
		String username = user.getUsername();
		String receiver = getReceiver(user);
		if (StringUtils.isBlank(receiver))
			throw new ReceiverNotFoundException("No receiver found for: " + username);
		String verificationCode = RequestContext.getRequest().getParameter("verificationCode");
		if (verificationCode == null)
			verificationCode = RequestContext.getRequest().getParameter("password");
		boolean verified = verificationService.verify(receiver, verificationCode);
		if (!verified)
			throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
	}

}
