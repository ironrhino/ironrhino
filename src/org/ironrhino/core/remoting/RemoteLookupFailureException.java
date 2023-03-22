package org.ironrhino.core.remoting;

public class RemoteLookupFailureException extends RemoteAccessException {

	private static final long serialVersionUID = 8153543370593930046L;

	public RemoteLookupFailureException(String msg) {
		super(msg);
	}

	public RemoteLookupFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
