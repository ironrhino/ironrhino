package org.ironrhino.core.security.verfication.impl;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.security.verfication.ReceiverNotFoundException;
import org.ironrhino.core.security.verfication.VerificationCodeEnabled;
import org.ironrhino.core.security.verfication.VerificationCodeGenerator;
import org.ironrhino.core.security.verfication.VerificationCodeNotifier;
import org.ironrhino.core.security.verfication.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@VerificationCodeEnabled
@Component("verificationService")
@Slf4j
public class DefaultVerificationService implements VerificationService {

	private static final String CACHE_NAMESPACE = "verification";
	private static final String SUFFIX_RESEND = "$$resend";
	private static final String SUFFIX_THRESHOLD = "$$threshold";

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private VerificationCodeGenerator verficationCodeGenerator;

	@Autowired
	private VerificationCodeNotifier verificationCodeNotifier;

	@Value("${verification.code.length:6}")
	private int length = 6;

	@Getter
	@Value("${verification.code.resend.interval:60}")
	private int resendInterval = 60;

	@Value("${verification.code.verify.interval:5}")
	private int verifyInterval = 5;

	@Getter
	@Value("${verification.code.verify.max.attempts:5}")
	private int maxAttempts = 5;

	@Value("${verification.code.expiry:300}")
	private int expiry = 300;

	@Value("${verification.code.reuse:true}")
	private boolean reuse = true;

	@Override
	public void send(String receiver, final String verficationCode) throws ReceiverNotFoundException {
		String codeToSend;
		if (verficationCode == null) {
			codeToSend = (String) cacheManager.get(receiver, CACHE_NAMESPACE);
			if (codeToSend != null && cacheManager.exists(receiver + SUFFIX_RESEND, CACHE_NAMESPACE)) {
				log.warn("{} is trying resend within cooldown time", receiver);
				return;
			}
		} else {
			codeToSend = verficationCode;
			cacheManager.put(receiver, codeToSend, expiry, TimeUnit.SECONDS, CACHE_NAMESPACE);
		}
		if (codeToSend == null || !reuse) {
			codeToSend = verficationCodeGenerator.generator(receiver, length);
			cacheManager.put(receiver, codeToSend, expiry, TimeUnit.SECONDS, CACHE_NAMESPACE);
		}
		verificationCodeNotifier.send(receiver, codeToSend);
		cacheManager.put(receiver + SUFFIX_RESEND, "", resendInterval, TimeUnit.SECONDS, CACHE_NAMESPACE);
	}

	@Override
	public boolean verify(String receiver, String verificationCode) {
		boolean verified = verificationCode != null
				&& verificationCode.equals(cacheManager.get(receiver, CACHE_NAMESPACE));
		if (!verified) {
			long times = cacheManager.increment(receiver + SUFFIX_THRESHOLD, 1, expiry, TimeUnit.SECONDS,
					CACHE_NAMESPACE);
			if (times >= maxAttempts) {
				cacheManager.mdelete(Arrays.asList(receiver, receiver + SUFFIX_THRESHOLD), CACHE_NAMESPACE);
			}
		} else {
			cacheManager.mdelete(Arrays.asList(receiver, receiver + SUFFIX_THRESHOLD), CACHE_NAMESPACE);
		}
		return verified;
	}

}
