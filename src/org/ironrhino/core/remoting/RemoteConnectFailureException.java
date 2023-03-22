package org.ironrhino.core.remoting;

public class RemoteConnectFailureException extends RemoteAccessException {

	private static final long serialVersionUID = -5270366097347616012L;

	public RemoteConnectFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
