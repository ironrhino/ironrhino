package org.ironrhino.core.security.captcha.impl;

import javax.servlet.http.HttpServletRequest;

public class SessionBasedCaptchaManager extends DefaultCaptchaManager {

	@Override
	protected String getThresholdKey(HttpServletRequest request) {
		return CACHE_PREFIX_THRESHOLD + request.getSession().getId();
	}

}
