package org.ironrhino.core.security.verfication;

public interface VerificationCodeNotifier {

	public void send(String receiver, String code);

}
