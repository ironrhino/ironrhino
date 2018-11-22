package org.ironrhino.core.spring.configuration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;

@Order(0)
@ClassPresentConditional("org.springframework.retry.annotation.RetryConfiguration")
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class RetryConfiguration extends org.springframework.retry.annotation.RetryConfiguration {

	private static final long serialVersionUID = -5711384379539881750L;

	@Override
	public int getOrder() {
		return -100;
	}

}