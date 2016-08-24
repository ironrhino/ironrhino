package org.ironrhino.core.security.captcha.impl;

import javax.servlet.http.HttpServletRequest;

public class SessionBasedCaptchaManager extends DefaultCaptchaManager {

	@Override
	protected String getCountKey(HttpServletRequest request) {
		return CACHE_PREFIX_COUNT + request.getSession().getId();
	}

}
