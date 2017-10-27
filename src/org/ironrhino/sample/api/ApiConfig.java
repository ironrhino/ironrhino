package org.ironrhino.sample.api;

import org.ironrhino.rest.ApiConfigBase;
import org.ironrhino.sample.api.interceptor.LoggingInteceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

//only for exclude-filter of root ctx
@ControllerAdvice
@Configuration
@ComponentScan
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ApiConfig extends ApiConfigBase {

	@Bean
	public LoggingInteceptor loggingInteceptor() {
		return new LoggingInteceptor();
	}

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loggingInteceptor());
	}
}