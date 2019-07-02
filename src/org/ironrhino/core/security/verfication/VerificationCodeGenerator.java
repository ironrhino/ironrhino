package org.ironrhino.core.security.verfication;

public interface VerificationCodeGenerator {

	String generator(String receiver, int length);

}
