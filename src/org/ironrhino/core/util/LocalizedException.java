package org.ironrhino.core.util;

import org.ironrhino.core.struts.I18N;

public class LocalizedException extends RuntimeException {

	private static final long serialVersionUID = -6195528283741934679L;

	public LocalizedException() {
		super();
	}

	public LocalizedException(String message) {
		super(message);
	}

	@Override
	public String getLocalizedMessage() {
		try {
			String message = getMessage();
			return I18N.getText(getClass().getName(),
					message != null ? new Object[] { I18N.getText(message, null) } : null);
		} catch (IllegalArgumentException e) {
			return getMessage();
		}
	}

}
