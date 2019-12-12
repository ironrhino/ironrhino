package org.ironrhino.core.security.action;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
import org.ironrhino.core.security.verfication.impl.DefaultVerificationCodeChecker;
import org.ironrhino.core.spring.configuration.CommonConfiguration;
import org.ironrhino.core.spring.security.DefaultUsernamePasswordAuthenticationFilter;
import org.ironrhino.core.spring.security.VerificationCodeRequirementService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
		assertThat(response.getStatus(), is(HttpStatus.FOUND.value()));
		assertThat(response.getHeader("Location"), is("/login"));
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
		assertThat(doc, is(notNullValue()));

		Element loginForm = doc.getElementById("login");
		assertThat(loginForm, is(notNullValue()));
		assertThat(loginForm.attr("name"), is("login"));
		assertThat(loginForm.attr("action"), is("/login"));
		assertThat(loginForm.attr("method"), is("post"));

		Element usernameInput = loginForm.getElementById("login-username");
		assertThat(usernameInput, is(notNullValue()));
		assertThat(usernameInput.attr("name"), is("username"));
		assertThat(usernameInput.tagName(), is("input"));
		assertThat(usernameInput.attr("value"), is("test"));

		if (!verificationCodeEnabled || passwordRequired || !passwordRequired && !verificationCodeRequired) {
			Element passwordInput = loginForm.getElementById("login-password");
			assertThat(passwordInput, is(notNullValue()));
			assertThat(passwordInput.attr("name"), is("password"));
			assertThat(passwordInput.tagName(), is("input"));
		} else {
			assertThat(loginForm.getElementById("login-password"), is(nullValue()));
		}

		if (verificationCodeEnabled && verificationCodeRequired) {
			Element verificationCodeInput = loginForm.getElementById("login-verificationCode");
			assertThat(verificationCodeInput, is(notNullValue()));
			assertThat(verificationCodeInput.attr("name"), is("verificationCode"));
			assertThat(verificationCodeInput.tagName(), is("input"));
		} else {
			assertThat(loginForm.getElementById("login-verificationCode"), is(nullValue()));
			Element remembermeInput = loginForm.getElementById("login-rememberme");
			assertThat(remembermeInput, is(notNullValue()));
			assertThat(remembermeInput.attr("name"), is("rememberme"));
			assertThat(remembermeInput.tagName(), is("input"));
		}

		Elements submits = loginForm.getElementsByAttributeValue("type", "submit");
		assertThat(submits, is(notNullValue()));
		assertThat(submits.size(), is(1));
		assertThat(submits.get(0).tagName(), is("button"));
	}

	@Test
	public void testGetVerificationCode() throws IOException {
		request.setMethod("GET");
		String output = executeAction("/login/sendVerificationCode");
		JsonNode jsonNode = JsonUtils.getObjectMapper().readTree(output);
		JsonNode errorMessage = jsonNode.at("/actionErrors/0");
		assertThat(errorMessage, is(notNullValue()));
		assertThat(errorMessage.asText(), is(getText("validation.error")));
	}

	@Test
	public void testSendVerificationCodeWithLocked() throws IOException {
		request.setMethod("POST");
		request.setParameter("username", "locked");
		willThrow(new LockedException("locked")).given(verificationManager).send("locked");
		String output = executeAction("/login/sendVerificationCode");
		JsonNode errorMessage = JsonUtils.getObjectMapper().readTree(output).at("/fieldErrors/username/0");
		assertThat(errorMessage, is(notNullValue()));
		assertThat(errorMessage.asText(), is(getText(LockedException.class.getName())));
	}

	@Test
	public void testSendVerificationCodeWithAccountExpired() throws IOException {
		request.setMethod("POST");
		request.setParameter("username", "expired");
		willThrow(new AccountExpiredException("expired")).given(verificationManager).send("expired");
		String output = executeAction("/login/sendVerificationCode");
		JsonNode errorMessage = JsonUtils.getObjectMapper().readTree(output).at("/fieldErrors/username/0");
		assertThat(errorMessage, is(notNullValue()));
		assertThat(errorMessage.asText(), is(getText(AccountExpiredException.class.getName())));
	}

	@Test
	public void testSendVerificationCode() throws IOException {
		request.setMethod("POST");
		request.setParameter("username", "test");
		String output = executeAction("/login/sendVerificationCode");
		then(verificationManager).should().send("test");
		JsonNode message = JsonUtils.getObjectMapper().readTree(output).at("/actionSuccessMessage");
		assertThat(message, is(notNullValue()));
		assertThat(message.asText(), is(getText("send.success")));
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
		assertThat(response.getStatus(), is(HttpStatus.FOUND.value()));
		assertThat(response.getHeader("Location"), is("/"));
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
		assertThat(errorMessage.asText(), is(getText(expected.getName())));
		assertThat(response.getStatus(), is(HttpStatus.OK.value()));

		initServletMockObjects();
	}

	@Override
	protected String getConfigPath() {
		return "struts-test.xml";
	}

	@Configuration
	@Import(CommonConfiguration.class)
	static class LoginActionConfig {

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

		@Bean
		public DefaultVerificationCodeChecker verificationCodeChecker() {
			return new DefaultVerificationCodeChecker();
		}

		@Bean
		public VerificationCodeRequirementService verificationCodeRequirementService() {
			return new VerificationCodeRequirementService();
		}
	}
}
