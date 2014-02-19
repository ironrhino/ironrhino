package org.ironrhino.core.security.captcha.impl;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.security.captcha.CaptchaManager;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("captchaManager")
public class DefaultCaptchaManager implements CaptchaManager {

	private static final char[] CHINESE_NUMBERS = "零壹贰叁肆伍陆柒捌玖".toCharArray();

	private static final String REQUEST_ATTRIBUTE_KEY_CAPTACHA_THRESHOLD_ADDED = "CAPTACHA_THRESHOLD_ADDED";
	private static final String REQUEST_ATTRIBUTE_KEY_CAPTACHA_REQUIRED = "CAPTACHA_REQUIRED";
	private static final String REQUEST_ATTRIBUTE_KEY_CAPTACHA_VALIDATED = "CAPTACHA_VALIDATED";

	private static final String CACHE_PREFIX_ANSWER = "answer_";

	public static final String CACHE_PREFIX_THRESHOLD = "captchaThreshold_";

	public static final int CACHE_ANSWER_TIME_TO_LIVE = 60;

	public static final int CACHE_THRESHOLD_TIME_TO_LIVE = 3600;

	@Autowired
	protected CacheManager cacheManager;

	@Override
	public String getChallenge(HttpServletRequest request, String token) {
		String challenge = String.valueOf(ThreadLocalRandom.current().nextInt(
				8999) + 1000);// width=60
		String answer = answer(challenge);
		cacheManager.put(CACHE_PREFIX_ANSWER + token, answer, -1,
				CACHE_ANSWER_TIME_TO_LIVE, TimeUnit.SECONDS, KEY_CAPTCHA);
		return challenge;
	}

	@Override
	public String fuzzifyChallenge(String challenge) {
		char[] chars = challenge.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (char c : chars)
			sb.append(CHINESE_NUMBERS[Integer.parseInt(String.valueOf(c))]);
		return sb.toString();
	}

	@Override
	public String clarifyChallenge(String input) {
		if (StringUtils.isNumeric(input))
			return input;
		char[] chars = input.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (char c : chars) {
			for (int i = 0; i < CHINESE_NUMBERS.length; i++) {
				if (c == CHINESE_NUMBERS[i]) {
					sb.append(String.valueOf(i));
					break;
				}
			}
		}
		return sb.toString();
	}

	protected String answer(String challenge) {
		return challenge;
	}

	protected boolean validate(String input, String answer) {
		if (input == null || answer == null)
			return false;
		return clarifyChallenge(input).equals(answer);
	}

	@Override
	public void addCaptachaThreshold(HttpServletRequest request) {
		boolean added = request
				.getAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_THRESHOLD_ADDED) != null;
		if (!added) {
			String key = getThresholdKey(request);
			Integer threshold = (Integer) cacheManager.get(key, KEY_CAPTCHA);
			if (threshold != null)
				threshold += 1;
			else
				threshold = 1;
			cacheManager.put(key, threshold, -1, CACHE_THRESHOLD_TIME_TO_LIVE,
					TimeUnit.SECONDS, KEY_CAPTCHA);
			request.setAttribute(
					REQUEST_ATTRIBUTE_KEY_CAPTACHA_THRESHOLD_ADDED, true);
		}

	}

	@Override
	public boolean[] isCaptchaRequired(HttpServletRequest request,
			Captcha captcha) {
		boolean[] required = (boolean[]) request
				.getAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_REQUIRED);
		if (required == null) {
			if (captcha != null) {
				if (captcha.always()) {
					required = new boolean[] { true, false };
				} else if (captcha.bypassLoggedInUser()) {
					required = new boolean[] {
							AuthzUtils.getUserDetails() == null, false };
				} else {
					Integer threshold = (Integer) cacheManager.get(
							getThresholdKey(request), KEY_CAPTCHA);
					if (threshold != null && threshold >= captcha.threshold()) {
						required = new boolean[] {
								true,
								(threshold > 0 && threshold == captcha
										.threshold()) };
					} else {
						required = new boolean[] { false, false };
					}
				}
			} else {
				required = new boolean[] { false, false };
			}
			request.setAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_REQUIRED,
					required);
		}
		return required;
	}

	@Override
	public boolean validate(HttpServletRequest request, String token) {
		Boolean validated = (Boolean) request
				.getAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_VALIDATED);
		if (validated == null) {
			String answer = (String) cacheManager.get(CACHE_PREFIX_ANSWER
					+ token, KEY_CAPTCHA);
			validated = validate(request.getParameter(KEY_CAPTCHA), answer);
			request.setAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_VALIDATED,
					validated);
		}
		if (validated)
			cacheManager.delete(getThresholdKey(request), KEY_CAPTCHA);
		else
			addCaptachaThreshold(request);
		return validated;
	}

	protected String getThresholdKey(HttpServletRequest request) {
		return CACHE_PREFIX_THRESHOLD + RequestUtils.getRemoteAddr(request);
	}
}
