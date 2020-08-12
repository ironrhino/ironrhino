package org.ironrhino.core.security.verfication;

@FunctionalInterface
public interface VerificationCodeNotifier {

	void send(String receiver, String code);

}
