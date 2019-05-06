package org.ironrhino.core.security.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.struts2.StrutsSpringJUnit4TestCase;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.freemarker.FreemarkerConfigurer;
import org.ironrhino.core.security.action.LoginActionTest.LoginActionConfig;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.spring.configuration.CommonConfiguration;
import org.ironrhino.core.spring.security.DefaultUsernamePasswordAuthenticationFilter;
import org.ironrhino.core.spring.security.WrongVerificationCodeException;
import org.ironrhino.core.util.JsonUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = LoginActionConfig.class)
@TestPropertySource(properties = "verification.code.enabled=true")
public class LoginActionTest extends StrutsSpringJUnit4TestCase<LoginAction> {

	@Value("${verification.code.enabled}")
	private boolean verificationCodeEnabled;
	@Autowired
	private VerificationManager verificationManager;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private DefaultUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Test
	public void testInput() {
		executeAction("/login/input");
		assertEquals(HttpStatus.FOUND.value(), response.getStatus());
		assertEquals("/login", response.getHeader("Location"));
	}

	@Test
	public void testLoginPageWithoutVerificationCode() {
		verifyLoginPage(true, false);
	}

	@Test
	public void testLoginPageWithVerificationCode() {
		verifyLoginPage(true, true);
	}

	@Test
	public void testLoginPageWithoutPassword() {
		verifyLoginPage(false, true);
	}

	@Test
	public void testLoginPageWithoutPasswordAndVerificationCode() {
		verifyLoginPage(false, false);
	}

	private void verifyLoginPage(boolean passwordRequired, boolean verificationCodeRequired) {
		request.setParameter("username", "test");
		given(verificationManager.isPasswordRequired("test")).willReturn(passwordRequired);
		given(verificationManager.isVerificationRequired("test")).willReturn(verificationCodeRequired);

		String output = executeAction("/login");
		Document doc = Jsoup.parse(output);
		assertNotNull(doc);

		Element loginForm = doc.getElementById("login");
		assertNotNull(loginForm);
		assertEquals("login", loginForm.attr("name"));
		assertEquals("/login", loginForm.attr("action"));
		assertEquals("post", loginForm.attr("method"));

		Element usernameInput = loginForm.getElementById("login-username");
		assertNotNull(usernameInput);
		assertEquals("username", usernameInput.attr("name"));
		assertEquals("input", usernameInput.tagName());
		assertEquals("test", usernameInput.attr("value"));

		if (!verificationCodeEnabled || passwordRequired) {
			Element passwordInput = loginForm.getElementById("login-password");
			assertNotNull(passwordInput);
			assertEquals("password", passwordInput.attr("name"));
			assertEquals("input", passwordInput.tagName());
		} else {
			assertNull(loginForm.getElementById("login-password"));
		}

		if (verificationCodeEnabled && verificationCodeRequired) {
			Element verificationCodeInput = loginForm.getElementById("login-verificationCode");
			assertNotNull(verificationCodeInput);
			assertEquals("verificationCode", verificationCodeInput.attr("name"));
			assertEquals("input", verificationCodeInput.tagName());
		} else {
			assertNull(loginForm.getElementById("login-verificationCode"));
			Element remembermeInput = loginForm.getElementById("login-rememberme");
			assertNotNull(remembermeInput);
			assertEquals("rememberme", remembermeInput.attr("name"));
			assertEquals("input", remembermeInput.tagName());
		}

		Elements submits = loginForm.getElementsByAttributeValue("type", "submit");
		assertNotNull(submits);
		assertEquals(1, submits.size());
		assertEquals("button", submits.get(0).tagName());
	}

	@Test
	public void testGetVerificationCode() throws IOException {
		request.setMethod("GET");
		String output = executeAction("/login/sendVerificationCode");
		JsonNode jsonNode = JsonUtils.getObjectMapper().readTree(output);
		JsonNode errorMessage = jsonNode.at("/actionErrors/0");
		assertNotNull(errorMessage);
		assertEquals(getText("validation.error"), errorMessage.asText());
	}

	@Test
	public void testSendVerificationCodeWithLocked() throws IOException {
		request.setMethod("POST");
		request.setParameter("username", "locked");
		willThrow(new LockedException("locked")).given(verificationManager).send("locked");
		String output = executeAction("/login/sendVerificationCode");
		JsonNode errorMessage = JsonUtils.getObjectMapper().readTree(output).at("/fieldErrors/username/0");
		assertNotNull(errorMessage);
		assertEquals(getText(LockedException.class.getName()), errorMessage.asText());
	}

	@Test
	public void testSendVerificationCodeWithAccountExpired() throws IOException {
		request.setMethod("POST");
		request.setParameter("username", "expired");
		willThrow(new AccountExpiredException("expired")).given(verificationManager).send("expired");
		String output = executeAction("/login/sendVerificationCode");
		JsonNode errorMessage = JsonUtils.getObjectMapper().readTree(output).at("/fieldErrors/username/0");
		assertNotNull(errorMessage);
		assertEquals(getText(AccountExpiredException.class.getName()), errorMessage.asText());
	}

	@Test
	public void testSendVerificationCode() throws IOException {
		request.setMethod("POST");
		request.setParameter("username", "test");
		String output = executeAction("/login/sendVerificationCode");
		then(verificationManager).should().send("test");
		JsonNode message = JsonUtils.getObjectMapper().readTree(output).at("/actionSuccessMessage");
		assertNotNull(message);
		assertEquals(getText("send.success"), message.asText());
	}

	@Test
	public void testLogin() throws IOException, ServletException {
		request.setMethod("POST");
		request.setParameter("username", "test");
		request.setParameter("password", "password");

		Authentication auth = mock(Authentication.class);
		given(auth.getPrincipal()).willReturn("test");
		given(auth.getName()).willReturn("test");
		given(auth.isAuthenticated()).willReturn(true);
		given(authenticationManager.authenticate(argThat(authentication -> "test".equals(authentication.getName()))))
				.willReturn(auth);

		executeAction("/login");

		then(usernamePasswordAuthenticationFilter).should().success(request, response, auth);
		assertEquals(HttpStatus.FOUND.value(), response.getStatus());
		assertEquals("/", response.getHeader("Location"));
	}

	@Test
	public void testLoginWithAuthenticationException() throws IOException, ServletException {
		testLoginWithAuthenticationException(DisabledException.class, "username");
		testLoginWithAuthenticationException(UsernameNotFoundException.class, "username");
		testLoginWithAuthenticationException(LockedException.class, "username");
		testLoginWithAuthenticationException(AccountExpiredException.class, "username");
		testLoginWithAuthenticationException(BadCredentialsException.class, "password");
		testLoginWithAuthenticationException(CredentialsExpiredException.class, "password");
		testLoginWithAuthenticationException(WrongVerificationCodeException.class, "verificationCode");
	}

	private void testLoginWithAuthenticationException(Class<? extends AuthenticationException> expected,
			String errorField) throws IOException, ServletException {
		request.setMethod("POST");
		request.addHeader("X-Data-Type", "json");
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		request.setParameter("username", "test");
		request.setParameter("password", "password");

		willThrow(expected).given(authenticationManager)
				.authenticate(argThat(authentication -> "test".equals(authentication.getName())));

		String output = executeAction("/login");
		then(usernamePasswordAuthenticationFilter).should().unsuccess(eq(request), eq(response), any(expected));
		JsonNode errorMessage = JsonUtils.getObjectMapper().readTree(output).at("/fieldErrors/" + errorField + "/0");
		assertEquals(getText(expected.getName()), errorMessage.asText());
		assertEquals(HttpStatus.OK.value(), response.getStatus());

		initServletMockObjects();
	}

	@Override
	protected String getConfigPath() {
		return "struts-test.xml";
	}

	@Configuration
	static class LoginActionConfig {

		@Bean
		public LocalValidatorFactoryBean validatorFactory() {
			return new CommonConfiguration().validatorFactory();
		}

		@Bean
		public MessageSource messageSource() {
			return new CommonConfiguration().messageSource();
		}

		@Bean
		public FreemarkerConfigurer freemarkerConfigurer() {
			return new FreemarkerConfigurer();
		}

		@Bean
		public UserDetailsService userDetailsService() {
			return mock(UserDetailsService.class);
		}

		@Bean
		public DefaultUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter() {
			return mock(DefaultUsernamePasswordAuthenticationFilter.class);
		}

		@Bean
		public AuthenticationFailureHandler authenticationFailureHandler() {
			return mock(AuthenticationFailureHandler.class);
		}

		@Bean
		public AuthenticationSuccessHandler authenticationSuccessHandler() {
			return mock(AuthenticationSuccessHandler.class);
		}

		@Bean
		public AuthenticationManager authenticationManager() {
			return mock(AuthenticationManager.class);
		}

		@Bean
		public WebAuthenticationDetailsSource authenticationDetailsSource() {
			return mock(WebAuthenticationDetailsSource.class);
		}

		@Bean
		public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
			return mock(SessionAuthenticationStrategy.class);
		}

		@Bean
		public EventPublisher eventPublisher() {
			return mock(EventPublisher.class);
		}

		@Bean
		public VerificationManager verificationManager() {
			return mock(VerificationManager.class);
		}
	}
}
