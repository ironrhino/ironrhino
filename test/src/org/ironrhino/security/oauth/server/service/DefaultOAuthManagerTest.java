package org.ironrhino.security.oauth.server.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.Date;

import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.DefaultOAuthManagerTest.OAuthManagerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = OAuthManagerConfig.class)
@TestPropertySource(properties = { "oauth.authorization.maximumDevices=1" })
public class DefaultOAuthManagerTest {

	@Value("${oauth.authorization.maximumDevices}")
	private long maximumDevices;

	@Autowired
	private OAuthManager oauthManager;

	@Autowired
	private ClientManager clientManager;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Test
	public void testGrant() {
		Client client = client("clientId", "secret", true);
		given(clientManager.get("clientId")).willReturn(client);

		Authorization authorization = oauthManager.grant(client);

		then(authorizationManager).should()
				.save(argThat(auth -> "clientId".equals(auth.getClient()) && null == auth.getGrantor()));
		assertThat(authorization.getClient(), is("clientId"));
		assertThat(authorization.getResponseType(), is(ResponseType.token));
		assertThat(authorization.getGrantType(), is(GrantType.client_credentials));
		assertThat(authorization.getRefreshToken(), is(notNullValue()));
		assertThat(authorization.getModifyDate(), is(notNullValue()));
	}

	@Test
	public void testGrantWithGrantor() {
		Client client = client("clientId", "secret", true);
		given(clientManager.get("clientId")).willReturn(client);

		Authorization authorization = oauthManager.grant(client, "grantor");

		then(authorizationManager).should()
				.save(argThat(auth -> "clientId".equals(auth.getClient()) && "grantor".equals(auth.getGrantor())));
		assertThat(authorization.getClient(), is("clientId"));
		assertThat(authorization.getResponseType(), is(ResponseType.token));
		assertThat(authorization.getGrantor(), is("grantor"));
		assertThat(authorization.getGrantType(), is(GrantType.password));
		assertThat(authorization.getRefreshToken(), is(notNullValue()));
		assertThat(authorization.getModifyDate(), is(notNullValue()));
	}

	@Test
	public void testGrantWithGrantorAndDeviceId() {
		Client client = client("clientId", "secret", true);
		Authorization authorization = mock(Authorization.class);
		given(authorization.getClient()).willReturn("clientId");
		given(authorization.getDeviceId()).willReturn("deviceId");
		given(authorizationManager.findByCriteria(any())).willReturn(authorization);
		given(authorizationManager.detachedCriteria()).willReturn(mock(DetachedCriteria.class));

		assertThat(oauthManager.grant(client, "grantor", "deviceId", "deviceName"), is(authorization));

		then(authorization).should().setAccessToken(anyString());
		then(authorization).should().setRefreshToken(anyString());
		then(authorization).should().setModifyDate(any(Date.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReachedMaximumDevices() {
		Client client = client("clientId", "secret", true);
		Authorization authorization = mock(Authorization.class);
		given(authorization.getClient()).willReturn("clientId");
		given(authorization.getDeviceId()).willReturn("deviceId");
		// indicate the authorization is new
		given(authorizationManager.findByCriteria(any())).willReturn(null);
		given(authorizationManager.countByCriteria(any())).willReturn(maximumDevices);
		given(authorizationManager.detachedCriteria()).willReturn(mock(DetachedCriteria.class));

		oauthManager.grant(client, "grantor", "deviceId", "deviceName");
	}

	@Test
	public void testRefresh() {
		Client client = client("clientId", "password", true);
		given(clientManager.get("clientId")).willReturn(client);
		Authorization authorization = mock(Authorization.class);
		given(authorization.getClient()).willReturn("clientId");
		given(authorization.getDeviceId()).willReturn("deviceId");
		given(authorizationManager.findOne("refreshToken", "refreshToken")).willReturn(authorization);

		assertThat(oauthManager.refresh(client, "refreshToken"), is(authorization));
		then(authorization).should().setAccessToken(anyString());
		then(authorization).should().setRefreshToken(anyString());
		then(authorization).should().setModifyDate(any(Date.class));
	}

	@Test
	public void testRevoke() {
		Authorization authorization = new Authorization();
		given(authorizationManager.findByNaturalId("accessToken")).willReturn(authorization);

		assertThat(oauthManager.revoke("accessToken"), is(true));
		then(authorizationManager).should().delete(authorization);
	}

	@Test
	public void testReuse() {
		Authorization authorization = mock(Authorization.class);
		oauthManager.reuse(authorization);
		then(authorization).should().setCode(anyString());
		then(authorization).should().setModifyDate(any(Date.class));
		then(authorization).should().setLifetime(Authorization.DEFAULT_LIFETIME);
		then(authorizationManager).should().save(authorization);
	}

	@Test
	public void testDeny() {
		Authorization authorization = new Authorization();
		given(authorizationManager.get("authorizationId")).willReturn(authorization);
		oauthManager.deny("authorizationId");
		then(authorizationManager).should().delete(authorization);
	}

	protected static Client client(String clientId, String secret, boolean enabled) {
		Client client = new Client();
		client.setId(clientId);
		client.setSecret(secret);
		client.setEnabled(enabled);
		assertThat(client.getClientId(), is(clientId));
		return client;
	}

	static class OAuthManagerConfig {

		@Bean
		public OAuthManager oauthManager() {
			return new DefaultOAuthManager();
		}

		@Bean
		public ClientManager clientManager() {
			return mock(ClientManager.class);
		}

		@Bean
		public AuthorizationManager authorizationManager() {
			return mock(AuthorizationManager.class);
		}
	}

}
