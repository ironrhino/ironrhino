package org.ironrhino.core.security.verfication;

public interface VerificationCodeNotifier {

	void send(String receiver, String code);

}
