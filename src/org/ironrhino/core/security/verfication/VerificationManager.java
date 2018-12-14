package org.ironrhino.core.security.verfication;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;

public interface VerificationManager {

	boolean isVerificationRequired(String username);

	default boolean isVerificationRequired(UserDetails user) {
		return (user instanceof VerificationAware) && ((VerificationAware) user).isVerificationRequired();
	}

	boolean isPasswordRequired(String username);

	default boolean isPasswordRequired(UserDetails user) {
		boolean isVerificationAware = user instanceof VerificationAware;
		return !isVerificationAware && StringUtils.isNotBlank(user.getPassword())
				|| isVerificationAware && ((VerificationAware) user).isPasswordRequired();
	}

	default String getReceiver(UserDetails user) {
		String receiver = null;
		if (user instanceof VerificationAware)
			receiver = ((VerificationAware) user).getReceiver();
		if (StringUtils.isBlank(receiver))
			throw new ReceiverNotFoundException("No receiver found for: " + user.getUsername());
		return receiver;
	}

	void send(String username);

	boolean verify(UserDetails user, String verificationCode);

}
