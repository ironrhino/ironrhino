package org.ironrhino.rest.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestClientConfiguration {

	@Bean
	public RestClient restClient() {
		return new RestClient("http://localhost:8080/api", "http://localhost:8080/oauth/oauth2/token",
				"1IbhUczby8QGuxIcA3zUQF", "76IytucIQQgKWDG06xGPsA");
	}

	@Bean
	public RestApiRegistryPostProcessor restApiRegistryPostProcessor() {
		return new RestApiRegistryPostProcessor();
	}

}
