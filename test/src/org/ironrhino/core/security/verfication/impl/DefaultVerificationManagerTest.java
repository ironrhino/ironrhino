package org.ironrhino.core.security.verfication.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.ironrhino.core.security.verfication.ReceiverNotFoundException;
import org.ironrhino.core.security.verfication.VerificationAware;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.security.verfication.VerificationService;
import org.ironrhino.core.security.verfication.impl.DefaultVerificationManagerTest.VerificationConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = VerificationConfig.class)
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
		assertTrue(verificationManager.isVerificationRequired("test"));
		assertTrue(verificationManager.isVerificationRequired(verificationAware));

		given(verificationAware.isVerificationRequired()).willReturn(false);
		assertFalse(verificationManager.isVerificationRequired("test"));
		assertFalse(verificationManager.isVerificationRequired(verificationAware));
	}

	@Test
	public void testIsPasswordRequired() {
		VerificationAware verificationAware = mock(VerificationAware.class);
		given(userDetailsManager.loadUserByUsername("test")).willReturn(verificationAware);

		given(verificationAware.isPasswordRequired()).willReturn(true);
		assertTrue(verificationManager.isPasswordRequired("test"));
		assertTrue(verificationManager.isPasswordRequired(verificationAware));

		given(verificationAware.isPasswordRequired()).willReturn(false);
		assertFalse(verificationManager.isPasswordRequired("test"));
		assertFalse(verificationManager.isPasswordRequired(verificationAware));
	}

	@Test
	public void testIsPasswordRequiredWithUserDetails() {
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetailsManager.loadUserByUsername("test")).willReturn(userDetails);

		given(userDetails.getPassword()).willReturn("password");
		assertTrue(verificationManager.isPasswordRequired("test"));
		assertTrue(verificationManager.isPasswordRequired(userDetails));

		given(userDetails.getPassword()).willReturn("");
		assertFalse(verificationManager.isPasswordRequired("test"));
		assertFalse(verificationManager.isPasswordRequired(userDetails));
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
		assertNotNull(e);

		given(verificationAware.getReceiver()).willReturn("test");
		assertEquals("test", verificationManager.getReceiver(verificationAware));
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

	static class VerificationConfig {

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
