package org.ironrhino.core.security.verfication;

public interface VerificationManager {

	public boolean isVerificationRequired(String username);

	public boolean isPasswordRequired(String username);

	public void send(String username) throws ReceiverNotFoundException;

	public void verify(VerificationAware user) throws WrongVerificationCodeException;

}
