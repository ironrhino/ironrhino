package org.ironrhino.core.security.verfication;

public interface VerficationCodeGenerator {

	public String generator(String receiver, int length);

}
