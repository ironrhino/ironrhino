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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.server.controller.OAuth2ControllerTest.OAuth2Configuration;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = OAuth2Configuration.class)
public class OAuth2ControllerTest {

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

	@Test
	public void testTokenWithoutGrantType() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("client_id", "client_id_value")
				.param("client_secret", "client_secret_value");
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request")).andExpect(
						jsonPoint("/error_message").value("Required GrantType parameter 'grant_type' is not present"));
	}

	@Test
	public void testTokenWithoutClientId() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "authorization_code")
				.param("client_secret", "client_secret_value");
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'client_id' is not present"));
	}

	@Test
	public void testTokenWithoutClientSecret() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "authorization_code")
				.param("client_id", "client_id_value");
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request")).andExpect(
						jsonPoint("/error_message").value("Required String parameter 'client_secret' is not present"));
	}

	@Test
	public void testClientCredentialToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "client_credentials")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value");
		Authorization auth = mock(Authorization.class);
		given(auth.getAccessToken()).willReturn("access_token_value");
		given(auth.getRefreshToken()).willReturn("refresh_token_value");
		given(auth.getExpiresIn()).willReturn(3600);
		willReturn(auth).given(oauthManager).grant(
				argThat(c -> "client_id_value".equals(c.getId()) && "client_secret_value".equals(c.getSecret())),
				isNull(), isNull());

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(3600))
				.andExpect(jsonPoint("/refresh_token").value("refresh_token_value"))
				.andExpect(jsonPoint("/access_token").value("access_token_value"));
	}

	@Test
	public void testClientCredentialTokenWithInvalidRequest() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "client_credentials")
				.param("client_id", "invalid_client_id").param("client_secret", "invalid_client_secret");
		willThrow(new RuntimeException("test")).given(oauthManager).grant(
				argThat(c -> "invalid_client_id".equals(c.getId()) && "invalid_client_secret".equals(c.getSecret())),
				isNull(), isNull());
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("test"));
	}

	@Test
	public void testPasswordToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "password")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value")
				.param("username", "username").param("password", "password");
		Client client = mock(Client.class);
		Authentication authentication = mock(Authentication.class);
		UserDetails userDetails = mock(UserDetails.class);
		Authorization authorization = mock(Authorization.class);
		given(client.getClientId()).willReturn("client_id_value");
		given(client.getSecret()).willReturn("client_secret_value");
		given(userDetails.getUsername()).willReturn("username");
		given(authorization.getAccessToken()).willReturn("access_token_value");
		given(authorization.getRefreshToken()).willReturn("refresh_token_value");
		given(authorization.getExpiresIn()).willReturn(3600);
		given(authorization.getGrantor()).willReturn("grantor");
		given(oauthManager.findClientById("client_id_value")).willReturn(client);
		given(authenticationManager.authenticate(
				argThat(auth -> "username".equals(auth.getPrincipal()) && "password".equals(auth.getCredentials()))))
						.willReturn(authentication);
		given(userDetailsService.loadUserByUsername("username")).willReturn(userDetails);
		given(oauthManager.grant(
				argThat(c -> "client_id_value".equals(c.getClientId()) && "client_secret_value".equals(c.getSecret())),
				eq("username"), isNull(), isNull())).willReturn(authorization);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(3600))
				.andExpect(jsonPoint("/refresh_token").value("refresh_token_value"))
				.andExpect(jsonPoint("/access_token").value("access_token_value"));
	}

	@Test
	public void testPasswordTokenWithoutUsername() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "password")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value");
		Client client = mock(Client.class);
		given(client.getClientId()).willReturn("client_id_value");
		given(client.getSecret()).willReturn("client_secret_value");
		given(oauthManager.findClientById("client_id_value")).willReturn(client);
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'username' is not present"));
	}

	@Test
	public void testPasswordTokenWithoutPassword() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "password")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value")
				.param("username", "username");
		Client client = mock(Client.class);
		given(client.getClientId()).willReturn("client_id_value");
		given(client.getSecret()).willReturn("client_secret_value");
		given(oauthManager.findClientById("client_id_value")).willReturn(client);
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'password' is not present"));
	}

	@Test
	public void testPasswordTokenWithAuthenticationException() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "password")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value")
				.param("username", "username").param("password", "password");
		Client client = mock(Client.class);
		given(client.getClientId()).willReturn("client_id_value");
		given(client.getSecret()).willReturn("client_secret_value");
		given(oauthManager.findClientById("client_id_value")).willReturn(client);
		willThrow(new AccountExpiredException("expired")).given(authenticationManager).authenticate(
				argThat(auth -> "username".equals(auth.getPrincipal()) && "password".equals(auth.getCredentials())));

		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value(I18N.getText(AccountExpiredException.class.getName())));
	}

	@Test
	public void testAuthorizationCodeToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "authorization_code")
				.param("code", "code").param("client_id", "client_id_value")
				.param("client_secret", "client_secret_value");
		Authorization authorization = mock(Authorization.class);
		given(authorization.getExpiresIn()).willReturn(3600);
		given(authorization.getAccessToken()).willReturn("access_token_value");
		given(authorization.getRefreshToken()).willReturn("refresh_token_value");
		given(authorization.getGrantor()).willReturn("grantor");
		given(oauthManager.authenticate(eq("code"), any(Client.class))).willReturn(authorization);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(3600))
				.andExpect(jsonPoint("/access_token").value("access_token_value"))
				.andExpect(jsonPoint("/refresh_token").value("refresh_token_value"));
	}

	@Test
	public void testAuthorizationCodeTokenWithoutCode() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "authorization_code")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value");
		mockMvc.perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("Required String parameter 'code' is not present"));
	}

	@Test
	public void testRefreshToken() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/token").param("grant_type", "refresh_token")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value")
				.param("refresh_token", "refresh_token_value");
		Authorization authorization = mock(Authorization.class);
		given(authorization.getAccessToken()).willReturn("new_access_token_value");
		given(authorization.getRefreshToken()).willReturn("new_refresh_token_value");
		given(authorization.getExpiresIn()).willReturn(3600);
		given(oauthManager.refresh(
				argThat(c -> "client_id_value".equals(c.getClientId()) && "client_secret_value".equals(c.getSecret())),
				eq("refresh_token_value"))).willReturn(authorization);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/expires_in").value(3600))
				.andExpect(jsonPoint("/refresh_token").value("new_refresh_token_value"))
				.andExpect(jsonPoint("/access_token").value("new_access_token_value"));
	}

	@Test
	public void testInfo() throws Exception {
		Authorization authorization = mock(Authorization.class);
		given(authorization.getClient()).willReturn("client_id");
		given(authorization.getExpiresIn()).willReturn(3600);
		given(authorization.getGrantor()).willReturn("grantor");
		given(oauthManager.retrieve("access_token")).willReturn(authorization);
		mockMvc.perform(get("/oauth2/info").param("access_token", "access_token")).andExpect(status().isOk())
				.andExpect(jsonPoint("/client_id").value("client_id"))
				.andExpect(jsonPoint("/username").value("grantor")).andExpect(jsonPoint("/expires_in").value(3600));
	}

	@Test
	public void testInfoWithInvalidToken() throws Exception {
		given(oauthManager.retrieve("access_token")).willReturn(null);
		mockMvc.perform(get("/oauth2/info").param("access_token", "access_token")).andExpect(status().isUnauthorized())
				.andExpect(jsonPoint("/error").value("invalid_token"));
	}

	@Test
	public void testInfoWithExpiredToken() throws Exception {
		Authorization authorization = mock(Authorization.class);
		given(authorization.getExpiresIn()).willReturn(-1);
		given(oauthManager.retrieve("access_token")).willReturn(authorization);
		mockMvc.perform(get("/oauth2/info").param("access_token", "access_token")).andExpect(status().isUnauthorized())
				.andExpect(jsonPoint("/error").value("invalid_token"))
				.andExpect(jsonPoint("/error_message").value("expired_token"));
	}

	@Test
	public void testRevokeWithInvalidToken() throws Exception {
		given(oauthManager.revoke("access_token")).willReturn(false);
		mockMvc.perform(get("/oauth2/revoke").param("access_token", "access_token")).andExpect(status().isBadRequest())
				.andExpect(jsonPoint("/error").value("invalid_request"))
				.andExpect(jsonPoint("/error_message").value("revoke_failed"));
	}

	@Test
	public void testSendVerificationCode() throws Exception {
		MockHttpServletRequestBuilder request = get("/oauth2/sendVerificationCode")
				.param("client_id", "client_id_value").param("client_secret", "client_secret_value")
				.param("username", "username");
		Client client = mock(Client.class);
		given(client.getClientId()).willReturn("client_id_value");
		given(client.getSecret()).willReturn("client_secret_value");
		given(oauthManager.findClientById("client_id_value")).willReturn(client);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPoint("/code").value(0))
				.andExpect(jsonPoint("/status").value("OK"));
		then(verificationManager).should().send("username");
	}

	@Configuration
	@EnableWebMvc
	static class OAuth2Configuration implements WebMvcConfigurer {

		@Bean
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

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			MappingJackson2HttpMessageConverter jackson2 = new MappingJackson2HttpMessageConverter() {

				@Override
				protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
						throws IOException, HttpMessageNotWritableException {
					super.writeInternal(object, type, outputMessage);
					if (!(outputMessage instanceof ServerHttpResponse)
							|| outputMessage instanceof ServletServerHttpResponse) {
						// don't close MediaType.TEXT_EVENT_STREAM
						outputMessage.getBody().close();
					}
				}

			};
			jackson2.setObjectMapper(JsonUtils.createNewObjectMapper());
			converters.add(jackson2);
			StringHttpMessageConverter string = new StringHttpMessageConverter(StandardCharsets.UTF_8) {

				@Override
				protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage)
						throws IOException {
					try {
						return super.readInternal(clazz, inputMessage);
					} finally {
						inputMessage.getBody().close();
					}
				}

				@Override
				protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
					super.writeInternal(str, outputMessage);
					if (!(outputMessage instanceof ServerHttpResponse)
							|| outputMessage instanceof ServletServerHttpResponse) {
						// don't close MediaType.TEXT_EVENT_STREAM
						outputMessage.getBody().close();
					}
				}

			};
			string.setWriteAcceptCharset(false);
			converters.add(string);
		}
	}

}
