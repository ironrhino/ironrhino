package org.ironrhino.rest.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.ironrhino.core.spring.http.client.RestTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestApiPlaceholderTest.RestApiConfiguration.class)
@TestPropertySource(properties = { "testClient.apiBaseUrl=invalidprotocol://localhost" })
public class RestApiPlaceholderTest {

	@Autowired
	private TestClient testClient;

	@Test
	public void test() {
		String error = null;
		try {
			testClient.echo("test");
		} catch (ResourceAccessException rae) {
			error = rae.getMessage();
		}
		assertThat(error, containsString("invalidprotocol"));
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
		public RestTemplate restTemplate() throws IOException {
			return new RestTemplate();
		}

	}

}
