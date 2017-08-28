package org.ironrhino.core.security.verfication;

public interface VerificationService {

	public default void send(String receiver) {
		send(receiver, null);
	}

	public void send(String receiver, String verificationCode);

	public boolean verify(String receiver, String verificationCode);

}
