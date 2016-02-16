package org.ironrhino.core.remoting.client;

import org.ironrhino.core.util.LocalizedException;

public class ServiceNotFoundException extends LocalizedException {

	private static final long serialVersionUID = -3712351114164696893L;

	public ServiceNotFoundException(String service) {
		super(service);
	}
}