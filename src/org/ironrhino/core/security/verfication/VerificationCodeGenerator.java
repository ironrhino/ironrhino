package org.ironrhino.core.security.verfication;

public interface VerificationCodeGenerator {

	public String generator(String receiver, int length);

}
