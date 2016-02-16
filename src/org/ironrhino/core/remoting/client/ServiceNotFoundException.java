package org.ironrhino.core.remoting.client;

public class ServiceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -3712351114164696893L;

	public ServiceNotFoundException() {
	}

	public ServiceNotFoundException(String msg) {
		super(msg);
	}
}