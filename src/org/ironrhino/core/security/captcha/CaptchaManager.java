package org.ironrhino.core.security.captcha;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.core.metadata.Captcha;

public interface CaptchaManager {

	String KEY_CAPTCHA = "captcha";

	String getChallenge(HttpServletRequest request, String token);

	boolean verify(HttpServletRequest request, String token, boolean cleanup);

	CaptchaStatus getCaptchaStatus(HttpServletRequest request, Captcha captcha);

	void addCaptchaCount(HttpServletRequest request);

}
