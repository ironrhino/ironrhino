package org.ironrhino.rest.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;

@Configuration
public class RestClientConfiguration {

	@Bean
	public RestClient restClient() {
		return new RestClient("http://localhost:8080/api", "http://localhost:8080/oauth/oauth2/token",
				"1IbhUczby8QGuxIcA3zUQF", "76IytucIQQgKWDG06xGPsA");
	}

	@Bean
	public RestApiRegistryPostProcessor restApiRegistryPostProcessor() {
		RestApiRegistryPostProcessor obj = new RestApiRegistryPostProcessor();
		obj.setPackagesToScan(new String[] { getClass().getPackage().getName() });
		return obj;
	}

	@Bean
	public MyJsonValidator myJsonValidator() {
		return new MyJsonValidator();
	}

	public static class MyJsonValidator implements JsonValidator {

		@Override
		public void validate(JsonNode tree) {
			if (tree.get("totalResults").asInt() > 0) {
				throw new IllegalArgumentException("Just for test");
			}
		}

	}

}
