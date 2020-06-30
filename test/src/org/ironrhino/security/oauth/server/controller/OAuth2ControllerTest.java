package org.ironrhino.security.oauth.server.controller;

import static org.ironrhino.rest.MockMvcResultMatchers.jsonPoint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.security.jwt.Jwt;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.rest.AbstractMockMvcConfigurer;
import org.ironrhino.security.oauth.server.component.OAuthHandler;
import org.ironrhino.security.oauth.server.controller.OAuth2ControllerTest.OAuth2Configuration;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthAuthorizationService;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = OAuth2Configuration.class)
@TestPropertySource(properties = { "oauth.token.jwtEnabled=true",
		"oauth.token.jwtExpiresIn=" + OAuth2ControllerTest.EXPIRES_IN })
public class OAuth2ControllerTest {

	public static final int EXPIRES_IN = 3600;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private OAuthManager oauthManager;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private UserDetailsService userDetailsService;
	@Autowired
	private VerificationManager verificationManager;

	final String username = "username";
	final String password = "password";
	final String clientId = "clientId";
	final String clientSecret = "clientSecret";
	final String grantor = "grantor";
	final String code = "code";
	final String accessToken = "accessToken";
	final String refreshToken = "refreshToken";
	int expiresIn = EXPIRES_IN;

	@Before
	public void init() {
		Mockito.reset(oauthManager, authenticationManager, userDetailsService);
	}

	@Test
	public void testTokenWithoutGrantType() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("client_id", clientId).param("client_secret",
				clientSecret);
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request")).andExpect(
						jsonPoint("/error_message").value("Required GrantType parameter 'grant_type' is not present"));
	}

	@Test
	public void testTokenWithoutClientId() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "authorization_code")
				.param("client_secret", clientSecret);
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'client_id' is not present"));
	}

	@Test
	public void testTokenWithoutClientSecret() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token")
				.param("grant_type", GrantType.authorization_code.name()).param("client_id", clientId);
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request")).andExpect(
						jsonPoint("/error_message").value("Required String parameter 'client_secret' is not present"));
	}

	@Test
	public void testClientCredentialToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token")
				.param("grant_type", GrantType.client_credentials.name()).param("client_id", clientId)
				.param("client_secret", clientSecret);
		Authorization auth = mock(Authorization.class);
		given(auth.getAccessToken()).willReturn(accessToken);
		given(auth.getRefreshToken()).willReturn(refreshToken);
		given(auth.getExpiresIn()).willReturn(expiresIn);
		willReturn(auth).given(oauthManager).grant(
				argThat(c -> clientId.equals(c.getId()) && clientSecret.equals(c.getSecret())), isNull(), isNull());

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(expiresIn))
				.andExpect(jsonPoint("/access_token").value(accessToken))
				.andExpect(jsonPoint("/refresh_token").value(refreshToken));
	}

	@Test
	public void testClientCredentialTokenWithInvalidRequest() throws Exception {
		String error = "Invalid client id";
		String invalidClientId = "invalid_client_id";
		String invalidClientSecret = "invalid_client_secret";
		MockHttpServletRequestBuilder request = get("/oauth2/token")
				.param("grant_type", GrantType.client_credentials.name()).param("client_id", invalidClientId)
				.param("client_secret", invalidClientSecret);
		willThrow(new RuntimeException(error)).given(oauthManager).grant(
				argThat(c -> invalidClientId.equals(c.getId()) && invalidClientSecret.equals(c.getSecret())), isNull(),
				isNull());
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value(error));
	}

	@Test
	public void testPasswordToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", GrantType.password.name())
				.param("client_id", clientId).param("client_secret", clientSecret).param("username", username)
				.param("password", password);
		mockClient();
		Authentication authentication = mock(Authentication.class);
		UserDetails userDetails = mock(UserDetails.class);
		Authorization authorization = mock(Authorization.class);
		given(userDetails.getUsername()).willReturn(username);
		given(authorization.getAccessToken()).willReturn(accessToken);
		given(authorization.getRefreshToken()).willReturn(refreshToken);
		given(authorization.getExpiresIn()).willReturn(expiresIn);
		given(authorization.getGrantor()).willReturn(grantor);
		given(authenticationManager.authenticate(
				argThat(auth -> username.equals(auth.getPrincipal()) && password.equals(auth.getCredentials()))))
						.willReturn(authentication);
		given(userDetailsService.loadUserByUsername(username)).willReturn(userDetails);
		given(oauthManager.grant(argThat(c -> clientId.equals(c.getClientId()) && clientSecret.equals(c.getSecret())),
				eq("username"), isNull(), isNull())).willReturn(authorization);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(expiresIn))
				.andExpect(jsonPoint("/access_token").value(accessToken))
				.andExpect(jsonPoint("/refresh_token").value(refreshToken));
	}

	@Test
	public void testPasswordTokenWithoutUsername() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", GrantType.password.name())
				.param("client_id", clientId).param("client_secret", clientSecret);
		mockClient();
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'username' is not present"));
	}

	@Test
	public void testPasswordTokenWithoutPassword() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", GrantType.password.name())
				.param("client_id", clientId).param("client_secret", clientSecret).param("username", username);
		mockClient();
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'password' is not present"));
	}

	@Test
	public void testPasswordTokenWithAuthenticationException() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", GrantType.password.name())
				.param("client_id", clientId).param("client_secret", clientSecret).param("username", username)
				.param("password", password);
		mockClient();
		willThrow(new AccountExpiredException("expired")).given(authenticationManager).authenticate(
				argThat(auth -> username.equals(auth.getPrincipal()) && password.equals(auth.getCredentials())));

		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value(I18N.getText(AccountExpiredException.class.getName())));
	}

	@Test
	public void testJwtBearerToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", GrantType.JWT_BEARER)
				.param("client_id", clientId).param("client_secret", clientSecret).param("username", username)
				.param("password", password);
		mockClient();
		Authentication authentication = mock(Authentication.class);
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetails.getUsername()).willReturn(username);
		given(userDetails.getPassword()).willReturn(password);
		given(authenticationManager.authenticate(
				argThat(auth -> username.equals(auth.getPrincipal()) && password.equals(auth.getCredentials()))))
						.willReturn(authentication);
		given(userDetailsService.loadUserByUsername(username)).willReturn(userDetails);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(expiresIn))
				.andExpect(jsonPoint("/access_token").test(String.class, jwt -> {
					Jwt.verifySignature(jwt, userDetails.getPassword());
					return true;
				}));

	}

	@Test
	public void testAuthorizationCodeToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token")
				.param("grant_type", GrantType.authorization_code.name()).param("code", code)
				.param("client_id", clientId).param("client_secret", clientSecret);
		Authorization authorization = mock(Authorization.class);
		given(authorization.getExpiresIn()).willReturn(expiresIn);
		given(authorization.getAccessToken()).willReturn(accessToken);
		given(authorization.getRefreshToken()).willReturn(refreshToken);
		given(authorization.getGrantor()).willReturn(grantor);
		given(oauthManager.authenticate(eq("code"), any(Client.class))).willReturn(authorization);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(expiresIn))
				.andExpect(jsonPoint("/access_token").value(accessToken))
				.andExpect(jsonPoint("/refresh_token").value(refreshToken));
	}

	@Test
	public void testAuthorizationCodeTokenWithoutCode() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token")
				.param("grant_type", GrantType.authorization_code.name()).param("client_id", clientId)
				.param("client_secret", clientSecret);
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'code' is not present"));
	}

	@Test
	public void testRefreshToken() throws Exception {
		String newAccessToken = "newAccessToken";
		String newRefreshToken = "newRefreshToken";
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", GrantType.refresh_token.name())
				.param("client_id", clientId).param("client_secret", clientSecret).param("refresh_token", refreshToken);
		Authorization authorization = mock(Authorization.class);
		given(authorization.getAccessToken()).willReturn(newAccessToken);
		given(authorization.getRefreshToken()).willReturn(newRefreshToken);
		given(authorization.getExpiresIn()).willReturn(expiresIn);
		given(oauthManager.refresh(argThat(c -> clientId.equals(c.getClientId()) && clientSecret.equals(c.getSecret())),
				eq(refreshToken))).willReturn(authorization);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(expiresIn))
				.andExpect(jsonPoint("/access_token").value(newAccessToken))
				.andExpect(jsonPoint("/refresh_token").value(newRefreshToken));
	}

	@Test
	public void testInfo() throws Exception {
		Authorization authorization = mock(Authorization.class);
		given(authorization.getClient()).willReturn(clientId);
		given(authorization.getExpiresIn()).willReturn(expiresIn);
		given(authorization.getGrantor()).willReturn(grantor);
		given(oauthManager.retrieve("access_token")).willReturn(authorization);
		mockMvc.perform(get("/oauth2/info").param("access_token", "access_token")).andExpect(status().isOk())
				.andExpect(jsonPoint("/client_id").value(clientId)).andExpect(jsonPoint("/username").value(grantor))
				.andExpect(jsonPoint("/expires_in").value(expiresIn));
	}

	@Test
	public void testInfoWithInvalidToken() throws Exception {
		given(oauthManager.retrieve(accessToken)).willReturn(null);
		mockMvc.perform(get("/oauth2/info").param("access_token", accessToken)).andExpect(status().isUnauthorized())
				.andExpect(jsonPoint("/error").value("invalid_token"));
	}

	@Test
	public void testInfoWithExpiredToken() throws Exception {
		Authorization authorization = mock(Authorization.class);
		given(authorization.getExpiresIn()).willReturn(-1);
		given(oauthManager.retrieve(accessToken)).willReturn(authorization);
		mockMvc.perform(get("/oauth2/info").param("access_token", accessToken)).andExpect(status().isUnauthorized())
				.andExpect(jsonPoint("/error").value("invalid_token"))
				.andExpect(jsonPoint("/error_message").value("expired_token"));
	}

	@Test
	public void testRevokeWithInvalidToken() throws Exception {
		given(oauthManager.revoke("access_token")).willReturn(false);
		mockMvc.perform(get("/oauth2/revoke").param("access_token", accessToken)).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("revoke_failed"));
	}

	@Test
	public void testSendVerificationCode() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/sendVerificationCode").param("client_id", clientId)
				.param("client_secret", clientSecret).param("username", username);
		mockClient();

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/code").value(0))
				.andExpect(jsonPoint("/status").value("OK"));
		then(verificationManager).should().send(username);
	}

	private Client mockClient() {
		Client client = mock(Client.class);
		given(client.getClientId()).willReturn(clientId);
		given(client.getSecret()).willReturn(clientSecret);
		given(oauthManager.findClientById(clientId)).willReturn(client);
		return client;
	}

	@EnableWebMvc
	static class OAuth2Configuration extends AbstractMockMvcConfigurer {

		@Bean
		public FormattingConversionService mvcConversionService() {
			DefaultFormattingConversionService defaultFormattingConversionService = new DefaultFormattingConversionService();
			defaultFormattingConversionService.addConverter(new Converter<String, GrantType>() {

				@Override
				public GrantType convert(String input) {
					if (StringUtils.isBlank(input))
						return null;
					try {
						return GrantType.valueOf(input);
					} catch (IllegalArgumentException e) {
						if (input.equals(GrantType.JWT_BEARER))
							return GrantType.jwt_bearer;
						throw e;
					}
				}

			});
			return defaultFormattingConversionService;
		}

		@Bean
		@Override
		public MockMvc mockMvc(WebApplicationContext wac) {
			return MockMvcBuilders.webAppContextSetup(wac)
					.alwaysExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).build();
		}

		@Bean
		public OAuth2Controller oauth2Controller() {
			return new OAuth2Controller();
		}

		@Bean
		public OAuth2ExceptionHandler oAuth2ExceptionHandler() {
			return new OAuth2ExceptionHandler();
		}

		@Bean
		public VerificationManager verificationManager() {
			return mock(VerificationManager.class);
		}

		@Bean
		public EventPublisher eventPublisher() {
			return mock(EventPublisher.class);
		}

		@Bean
		public OAuthManager oauthManager() {
			return mock(OAuthManager.class);
		}

		@Bean
		public OAuthHandler oauthHandler() {
			return new OAuthHandler();
		}

		@Bean
		public OAuthAuthorizationService oauthAuthorizationService() {
			return mock(OAuthAuthorizationService.class);
		}

		@Bean
		public HttpSessionManager httpSessionManager() {
			return mock(HttpSessionManager.class);
		}

		@Bean
		public UserDetailsService userDetailsService() {
			return mock(UserDetailsService.class);
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
	}
}
