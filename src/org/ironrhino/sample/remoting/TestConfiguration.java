package org.ironrhino.sample.remoting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

	@Bean
	public BarService barService() {
		return new BarServiceImpl();
	}

}
