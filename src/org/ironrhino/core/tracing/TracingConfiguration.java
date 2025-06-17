package org.ironrhino.core.tracing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class TracingConfiguration {

	@Bean
	TracedAspect tracingAspect() {
		return new TracedAspect();
	}

}
