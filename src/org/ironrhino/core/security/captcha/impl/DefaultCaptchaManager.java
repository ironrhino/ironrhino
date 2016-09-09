package org.ironrhino.core.security.captcha.impl;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.security.captcha.CaptchaManager;
import org.ironrhino.core.security.captcha.CaptchaStatus;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("captchaManager")
public class DefaultCaptchaManager implements CaptchaManager {

	private static final char[] CHINESE_NUMBERS = "零壹贰叁肆伍陆柒捌玖".toCharArray();

	private static final String REQUEST_ATTRIBUTE_KEY_CAPTACHA_THRESHOLD_ADDED = "CAPTACHA_THRESHOLD_ADDED";
	private static final String REQUEST_ATTRIBUTE_KEY_CAPTACHA_STATUS = "CAPTACHA_STATUS";
	private static final String REQUEST_ATTRIBUTE_KEY_CAPTACHA_VALIDATED = "CAPTACHA_VALIDATED";

	private static final String CACHE_PREFIX_ANSWER = "answer_";

	public static final String CACHE_PREFIX_COUNT = "captchaCount_";

	public static final int CACHE_ANSWER_TIME_TO_LIVE = 60;

	public static final int CACHE_THRESHOLD_TIME_TO_LIVE = 3600;

	@Value("${captchaManager.bypass:false}")
	private boolean bypass;

	@Autowired
	protected CacheManager cacheManager;

	@Override
	public String getChallenge(HttpServletRequest request, String token) {
		String challenge = String.valueOf(ThreadLocalRandom.current().nextInt(8999) + 1000);// width=60
		String answer = answer(challenge);
		cacheManager.put(CACHE_PREFIX_ANSWER + token, answer, -1, CACHE_ANSWER_TIME_TO_LIVE, TimeUnit.SECONDS,
				KEY_CAPTCHA);
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

	protected boolean verify(String input, String answer) {
		if (input == null || answer == null)
			return false;
		return clarifyChallenge(input).equals(answer);
	}

	@Override
	public void addCaptchaCount(HttpServletRequest request) {
		if (bypass)
			return;
		boolean added = request.getAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_THRESHOLD_ADDED) != null;
		if (!added) {
			String key = getCountKey(request);
			Integer threshold = (Integer) cacheManager.get(key, KEY_CAPTCHA);
			if (threshold != null)
				threshold += 1;
			else
				threshold = 1;
			cacheManager.put(key, threshold, -1, CACHE_THRESHOLD_TIME_TO_LIVE, TimeUnit.SECONDS, KEY_CAPTCHA);
			request.setAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_THRESHOLD_ADDED, true);
		}

	}

	@Override
	public CaptchaStatus getCaptchaStatus(HttpServletRequest request, Captcha captcha) {
		if (bypass)
			return CaptchaStatus.EMPTY;
		CaptchaStatus status = (CaptchaStatus) request.getAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_STATUS);
		if (status == null) {
			if (captcha != null) {
				if (captcha.always()) {
					status = new CaptchaStatus(true, captcha.threshold(), 0);
				} else if (captcha.bypassLoggedInUser()) {
					status = new CaptchaStatus(AuthzUtils.getUserDetails() == null, captcha.threshold(), 0);
				} else {
					Integer threshold = (Integer) cacheManager.get(getCountKey(request), KEY_CAPTCHA);
					if (threshold == null)
						threshold = 0;
					status = new CaptchaStatus(threshold >= captcha.threshold(), captcha.threshold(), threshold);
				}
			} else {
				status = CaptchaStatus.EMPTY;
			}
			request.setAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_STATUS, status);
		}
		return status;
	}

	@Override
	public boolean verify(HttpServletRequest request, String token, boolean cleanup) {
		Boolean pass = (Boolean) request.getAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_VALIDATED);
		if (pass == null) {
			String answer = (String) cacheManager.get(CACHE_PREFIX_ANSWER + token, KEY_CAPTCHA);
			pass = verify(request.getParameter(KEY_CAPTCHA), answer);
			request.setAttribute(REQUEST_ATTRIBUTE_KEY_CAPTACHA_VALIDATED, pass);
		}
		if (!cleanup)
			return pass;
		if (pass)
			cacheManager.delete(getCountKey(request), KEY_CAPTCHA);
		else
			addCaptchaCount(request);
		return pass;
	}

	protected String getCountKey(HttpServletRequest request) {
		return CACHE_PREFIX_COUNT + request.getRemoteAddr();
	}
}
