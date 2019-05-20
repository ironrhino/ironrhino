package org.ironrhino.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.ConnectException;

import org.ironrhino.core.spring.configuration.Fallback;
import org.ironrhino.core.spring.http.client.RestTemplate;
import org.ironrhino.rest.client.RestApiFallbackTest.RestApiConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestApiConfiguration.class)
@TestPropertySource(properties = { "org.ironrhino.rest.client.TestClient.apiBaseUrl=http://localhost/api" })
public class RestApiFallbackTest {

	@Autowired
	private TestClient testClient;

	@Test
	public void test() {
		assertFalse(testClient instanceof FallbackTestClient);
		assertTrue(Proxy.isProxyClass(testClient.getClass()));
		int errorCount = 0;
		for (int i = 0; i < 50; i++)
			try {
				testClient.echo("test");
			} catch (ResourceAccessException e) {
				errorCount++;
			}
		assertEquals(50, errorCount);
		for (int i = 0; i < 50; i++)
			try {
				testClient.echo("test");
			} catch (ResourceAccessException e) {
				errorCount++;
			}
		assertEquals(100, errorCount);
		// CircuitBreaker is open and fallback will active
		assertEquals("echo:test", testClient.echo("test"));
	}

	@Configuration
	static class RestApiConfiguration {

		@Bean
		public static RestApiRegistryPostProcessor restApiRegistryPostProcessor() {
			RestApiRegistryPostProcessor obj = new RestApiRegistryPostProcessor();
			obj.setAnnotatedClasses(new Class<?>[] { TestClient.class });
			return obj;
		}

		@Bean
		public FallbackTestClient fallbackTestClient() {
			return new FallbackTestClient();
		}

		@Bean
		public RestTemplate restTemplate() throws IOException {
			RestTemplate restTemplate = mock(RestTemplate.class);
			given(restTemplate.exchange(ArgumentMatchers.<RequestEntity<?>>any(),
					ArgumentMatchers.<ParameterizedTypeReference<Object>>any())).willThrow(
							new ResourceAccessException("I/O error on POST request for \"http://localhost/api/echo\"",
									new ConnectException("Connection refused: connect")));
			return restTemplate;
		}

	}

	@Fallback
	static class FallbackTestClient implements TestClient {

		@Override
		public String echo(String name) {
			return "echo:" + name;
		}

	}

}
