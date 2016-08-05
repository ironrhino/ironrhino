package org.ironrhino.core.spring.configuration;

import org.springframework.context.annotation.Configuration;

@ClassPresentConditional("org.springframework.retry.annotation.RetryConfiguration")
@Configuration
public class RetryConfiguration extends org.springframework.retry.annotation.RetryConfiguration {

	private static final long serialVersionUID = -5711384379539881750L;

	@Override
	public int getOrder() {
		return -100;
	}

}