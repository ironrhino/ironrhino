package org.ironrhino.core.security.verfication.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.security.verfication.ReceiverNotFoundException;
import org.ironrhino.core.security.verfication.VerificationCodeNotifier;
import org.ironrhino.core.security.verfication.VerificationService;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.util.CodecUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component("verificationService")
public class DefaultVerificationService implements VerificationService {

	private static final String CACHE_NAMESPACE = "verification";
	private static final String SUFFIX_RESEND = "$$resend";
	private static final String SUFFIX_THRESHOLD = "$$threshold";

	@Autowired
	private Logger logger;

	@Autowired
	private CacheManager cacheManager;

	@Autowired(required = false)
	private List<VerificationCodeNotifier> verificationCodeNotifiers;

	@Value("${verification.code.length:6}")
	private int length = 6;

	@Value("${verification.code.resend.interval:60}")
	private int resendInterval = 60;

	@Value("${verification.code.verify.interval:5}")
	private int verifyInterval = 5;

	@Value("${verification.code.verify.max.attempts:5}")
	private int maxAttempts = 5;

	@Value("${verification.code.expiry:300}")
	private int expiry = 300;

	@Value("${verification.code.reuse:true}")
	private boolean reuse = true;

	@Override
	public void send(String receiver) throws ReceiverNotFoundException {
		String verficationCode = (String) cacheManager.get(receiver, CACHE_NAMESPACE);
		if (verficationCode != null && cacheManager.exists(receiver + SUFFIX_RESEND, CACHE_NAMESPACE)) {
			logger.warn("{} is trying resend within cooldown time", receiver);
			return;
		}
		if (verficationCode == null || !reuse) {
			verficationCode = CodecUtils.randomDigitalString(length);
			cacheManager.put(receiver, verficationCode, expiry, TimeUnit.SECONDS, CACHE_NAMESPACE);
		}
		for (VerificationCodeNotifier notifier : verificationCodeNotifiers)
			notifier.send(receiver, verficationCode);
		cacheManager.put(receiver + SUFFIX_RESEND, "", resendInterval, TimeUnit.SECONDS, CACHE_NAMESPACE);
	}

	@Override
	public boolean verify(String receiver, String verificationCode) {
		boolean verified = verificationCode != null
				&& verificationCode.equals(cacheManager.get(receiver, CACHE_NAMESPACE));
		if (!verified) {
			cacheManager.delay(receiver, CACHE_NAMESPACE, verifyInterval, TimeUnit.SECONDS, verifyInterval / 2);
			Integer times = (Integer) cacheManager.get(receiver + SUFFIX_THRESHOLD, CACHE_NAMESPACE);
			if (times == null)
				times = 1;
			else
				times = times + 1;
			if (times >= maxAttempts) {
				cacheManager.delete(receiver, CACHE_NAMESPACE);
				cacheManager.delete(receiver + SUFFIX_THRESHOLD, CACHE_NAMESPACE);
			} else {
				cacheManager.put(receiver + SUFFIX_THRESHOLD, times, expiry, TimeUnit.SECONDS, CACHE_NAMESPACE);
			}
		} else {
			cacheManager.delete(receiver, CACHE_NAMESPACE);
			cacheManager.delete(receiver + SUFFIX_THRESHOLD, CACHE_NAMESPACE);
		}
		return verified;
	}

}
