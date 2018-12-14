package org.ironrhino.core.security.verfication;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;

public interface VerificationManager {

	public boolean isVerificationRequired(String username);

	public default boolean isVerificationRequired(UserDetails user) {
		return (user instanceof VerificationAware) && ((VerificationAware) user).isVerificationRequired();
	}

	public boolean isPasswordRequired(String username);

	public default boolean isPasswordRequired(UserDetails user) {
		boolean isVerificationAware = user instanceof VerificationAware;
		return !isVerificationAware && StringUtils.isNotBlank(user.getPassword())
				|| isVerificationAware && ((VerificationAware) user).isPasswordRequired();
	}

	public default String getReceiver(UserDetails user) {
		String receiver = null;
		if (user instanceof VerificationAware)
			receiver = ((VerificationAware) user).getReceiver();
		if (StringUtils.isBlank(receiver))
			throw new ReceiverNotFoundException("No receiver found for: " + user.getUsername());
		return receiver;
	}

	public void send(String username);

	public boolean verify(UserDetails user, String verificationCode);

}
