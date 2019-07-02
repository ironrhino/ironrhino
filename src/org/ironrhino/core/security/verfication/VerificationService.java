package org.ironrhino.core.security.verfication;

public interface VerificationService {

	default void send(String receiver) {
		send(receiver, null);
	}

	void send(String receiver, String verificationCode);

	boolean verify(String receiver, String verificationCode);

}
