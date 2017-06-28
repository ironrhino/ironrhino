package org.ironrhino.core.security.verfication;

public interface VerificationService {

	public void send(String receiver);

	public boolean verify(String receiver, String verificationCode);

}
