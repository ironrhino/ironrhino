package org.ironrhino.core.security.verfication;

@FunctionalInterface
public interface VerificationCodeGenerator {

	String generator(String receiver, int length);

}
