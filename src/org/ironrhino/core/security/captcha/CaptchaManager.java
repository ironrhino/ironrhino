package org.ironrhino.core.security.captcha;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.core.metadata.Captcha;

public interface CaptchaManager {

	String KEY_CAPTCHA = "captcha";

	public String getChallenge(HttpServletRequest request, String token);

	public boolean verify(HttpServletRequest request, String token, boolean cleanup);

	public CaptchaStatus getCaptchaStatus(HttpServletRequest request, Captcha captcha);

	public void addCaptchaCount(HttpServletRequest request);

}
