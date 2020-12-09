package org.ironrhino.core.security.verfication.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.ironrhino.core.security.verfication.ReceiverNotFoundException;
import org.ironrhino.core.security.verfication.VerificationAware;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.security.verfication.VerificationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultVerificationManagerTest.Config.class)
public class DefaultVerificationManagerTest {

	@Autowired
	private VerificationManager verificationManager;
	@Autowired
	private VerificationService verificationService;
	@Autowired
	private UserDetailsManager userDetailsManager;

	@Test
	public void testIsVerificationRequired() {
		VerificationAware verificationAware = mock(VerificationAware.class);
		given(userDetailsManager.loadUserByUsername("test")).willReturn(verificationAware);

		given(verificationAware.isVerificationRequired()).willReturn(true);
		assertThat(verificationManager.isVerificationRequired("test"), is(true));
		assertThat(verificationManager.isVerificationRequired(verificationAware), is(true));

		given(verificationAware.isVerificationRequired()).willReturn(false);
		assertThat(verificationManager.isVerificationRequired("test"), is(false));
		assertThat(verificationManager.isVerificationRequired(verificationAware), is(false));
	}

	@Test
	public void testIsPasswordRequired() {
		VerificationAware verificationAware = mock(VerificationAware.class);
		given(userDetailsManager.loadUserByUsername("test")).willReturn(verificationAware);

		given(verificationAware.isPasswordRequired()).willReturn(true);
		assertThat(verificationManager.isPasswordRequired("test"), is(true));
		assertThat(verificationManager.isPasswordRequired(verificationAware), is(true));

		given(verificationAware.isPasswordRequired()).willReturn(false);
		assertThat(verificationManager.isPasswordRequired("test"), is(false));
		assertThat(verificationManager.isPasswordRequired(verificationAware), is(false));
	}

	@Test
	public void testIsPasswordRequiredWithUserDetails() {
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetailsManager.loadUserByUsername("test")).willReturn(userDetails);

		given(userDetails.getPassword()).willReturn("password");
		assertThat(verificationManager.isPasswordRequired("test"), is(true));
		assertThat(verificationManager.isPasswordRequired(userDetails), is(true));

		given(userDetails.getPassword()).willReturn("");
		assertThat(verificationManager.isPasswordRequired("test"), is(false));
		assertThat(verificationManager.isPasswordRequired(userDetails), is(false));
	}

	@Test
	public void testGetReceiver() {
		VerificationAware verificationAware = mock(VerificationAware.class);

		given(verificationAware.getReceiver()).willReturn("");
		Exception e = null;
		try {
			verificationManager.getReceiver(verificationAware);
			fail("excepted exception");
		} catch (ReceiverNotFoundException error) {
			e = error;
		}
		assertThat(e, is(notNullValue()));

		given(verificationAware.getReceiver()).willReturn("test");
		assertThat(verificationManager.getReceiver(verificationAware), is("test"));
	}

	@Test
	public void testSend() {
		VerificationAware verificationAware = mock(VerificationAware.class);
		given(verificationAware.getUsername()).willReturn("test");
		given(verificationAware.getReceiver()).willReturn("test");
		given(userDetailsManager.loadUserByUsername("test")).willReturn(verificationAware);
		verificationManager.send("test");
		then(verificationService).should().send("test");
	}

	@Test
	public void testVerify() {
		VerificationAware verificationAware = mock(VerificationAware.class);
		given(verificationAware.getUsername()).willReturn("test");
		given(verificationAware.getReceiver()).willReturn("test");
		verificationManager.verify(verificationAware, "verificationCode");
		then(verificationService).should().verify("test", "verificationCode");
	}

	static class Config {

		@Bean
		public VerificationManager verificationManager() {
			return new DefaultVerificationManager();
		}

		@Bean
		public UserDetailsManager userDetailsManager() {
			return mock(UserDetailsManager.class);
		}

		@Bean
		public VerificationService verificationService() {
			return mock(VerificationService.class);
		}
	}
}
