package org.ironrhino.core.remoting;

import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.PersonRepository;
import org.ironrhino.sample.remoting.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class RemotingConfiguration {

	@Bean
	public HttpInvokerClient testService() {
		HttpInvokerClient hic = new HttpInvokerClient();
		hic.setServiceInterface(TestService.class);
		hic.setHost("localhost");
		return hic;
	}

	@Bean
	public HttpInvokerClient fooService() {
		HttpInvokerClient hic = new HttpInvokerClient();
		hic.setServiceInterface(FooService.class);
		hic.setHost("localhost");
		return hic;
	}

	@Bean
	public HttpInvokerClient barService() {
		HttpInvokerClient hic = new HttpInvokerClient();
		hic.setServiceInterface(BarService.class);
		hic.setHost("localhost");
		return hic;
	}

	@Bean
	public HttpInvokerClient personRepository() {
		HttpInvokerClient hic = new HttpInvokerClient();
		hic.setServiceInterface(PersonRepository.class);
		hic.setHost("localhost");
		return hic;
	}

	@Bean
	public LocalValidatorFactoryBean validatorFactory() {
		return new LocalValidatorFactoryBean();
	}

}
