package org.ironrhino.sample.retry;

import org.springframework.context.annotation.Configuration;

@SuppressWarnings("serial")
@Configuration
public class RetryConfiguration extends org.springframework.retry.annotation.RetryConfiguration {

	@Override
	public int getOrder() {
		return -100;
	}

}
