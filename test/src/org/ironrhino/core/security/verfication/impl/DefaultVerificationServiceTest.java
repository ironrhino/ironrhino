package org.ironrhino.core.security.verfication.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.ironrhino.core.security.verfication.VerificationCodeGenerator;
import org.ironrhino.core.security.verfication.VerificationCodeNotifier;
import org.ironrhino.core.security.verfication.VerificationService;
import org.ironrhino.core.security.verfication.impl.DefaultVerificationServiceTest.VerificationConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = VerificationConfig.class)
@TestPropertySource(properties = "verification.code.resend.interval=2")
public class DefaultVerificationServiceTest {

	@Autowired
	private CacheManager cacheManager;
	@Autowired
	private DefaultVerificationService verificationService;
	@Autowired
	private VerificationCodeNotifier verificationCodeNotifier;

	@Test
	public void testSend() {
		verificationService.send("testSend");
		then(verificationCodeNotifier).should().send(eq("testSend"), eq("testSend"));
	}

	@Test
	public void testResendWithinResendInterval() {
		verificationService.send("testResend");
		verificationService.send("testResend");
		then(verificationCodeNotifier).should().send(eq("testResend"), eq("testResend"));
	}

	@Test
	public void testResendOverResendInterval() throws InterruptedException {
		verificationService.send("testResend2");
		TimeUnit.MILLISECONDS.sleep(verificationService.getResendInterval() * 1000 + 100);
		verificationService.send("testResend2");
		then(verificationCodeNotifier).should(times(2)).send(eq("testResend2"), eq("testResend2"));
	}

	@Test
	public void testVerify() {
		verificationService.send("testVerify");
		then(verificationCodeNotifier).should().send(eq("testVerify"), eq("testVerify"));
		assertThat(verificationService.verify("testVerify", "testVerify"), is(true));
		then(cacheManager).should().mdelete(
				argThat(collection -> collection != null
						&& collection.containsAll(Arrays.asList("testVerify", "testVerify$$threshold"))),
				eq("verification"));
	}

	@Test
	public void testVerifyWithErrorCode() {
		verificationService.send("testVerifyWithErrorCode");
		then(verificationCodeNotifier).should().send(eq("testVerifyWithErrorCode"), eq("testVerifyWithErrorCode"));
		for (int i = 0; i < verificationService.getMaxAttempts(); i++) {
			assertThat(verificationService.verify("testVerifyWithErrorCode", "test"), is(false));
		}
		then(cacheManager).should().mdelete(
				argThat(collection -> collection != null && collection
						.containsAll(Arrays.asList("testVerifyWithErrorCode", "testVerifyWithErrorCode$$threshold"))),
				eq("verification"));
		assertThat(verificationService.verify("testVerifyWithErrorCode", "testVerifyWithErrorCode"), is(false));
	}

	static class VerificationConfig {

		@Bean
		public VerificationService verificationService() {
			return new DefaultVerificationService();
		}

		@Bean
		public CacheManager cacheManager() {
			return spy(new Cache2kCacheManager());
		}

		@Bean
		public VerificationCodeGenerator verificationCodeGenerator() {
			return (receiver, length) -> receiver;
		}

		@Bean
		public VerificationCodeNotifier verificationCodeNotifier() {
			return mock(VerificationCodeNotifier.class);
		}
	}
}
