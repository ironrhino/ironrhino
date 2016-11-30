package org.ironrhino.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.ironrhino.security.domain.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

public class RestClientTests {

	private static RestClient restClient;

	@BeforeClass
	public static void setup() {
		restClient = new RestClient("http://localhost:8080/oauth/oauth2/token", "1IbhUczby8QGuxIcA3zUQF",
				"76IytucIQQgKWDG06xGPsA");
	}

	@Test
	public void testFetchAccessToken() {
		String accessToken = restClient.fetchAccessToken();
		assertNotNull(accessToken);
		String accessToken2 = restClient.fetchAccessToken();
		assertNotNull(accessToken2);
		assertEquals(accessToken, accessToken2);
	}

	@Test
	public void testRestTemplate() {
		RestTemplate rt = restClient.getRestTemplate();
		User u = rt.getForObject("http://localhost:8080/api/user/@self", User.class);
		assertNotNull(u.getUsername());
		u = rt.getForObject("http://localhost:8080/api/user/admin", User.class);
		assertEquals("admin", u.getUsername());
	}

}
