package org.ironrhino.core.remoting;

public class RemoteInvocationFailureException extends RemoteAccessException {

	private static final long serialVersionUID = -9019282158212539947L;

	public RemoteInvocationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
