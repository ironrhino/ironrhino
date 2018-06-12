package org.ironrhino.core.util;

import org.ironrhino.core.struts.I18N;

public class LocalizedException extends RuntimeException {

	private static final long serialVersionUID = -6195528283741934679L;

	public LocalizedException(String message) {
		super(message);
	}

	public LocalizedException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

	public LocalizedException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getLocalizedMessage() {
		try {
			String message = getMessage();
			String key = getClass().getName();
			if (message == null && getCause() != null && getClass() == LocalizedException.class) {
				message = getCause().getMessage();
				key = getCause().getClass().getName();
			}
			return I18N.getText(key, message != null ? new Object[] { I18N.getText(message, null) } : null);
		} catch (IllegalArgumentException e) {
			return getMessage();
		}
	}

}
