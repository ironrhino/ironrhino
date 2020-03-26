package org.ironrhino.core.security.webauthn;

import org.ironrhino.core.util.LocalizedException;

public class CredentialExpiredException extends LocalizedException {

	private static final long serialVersionUID = 6030345081549043492L;

	public CredentialExpiredException(String message) {
		super(message);
	}

}
