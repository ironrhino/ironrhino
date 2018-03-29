package org.ironrhino.core.remoting;

import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.sample.remoting.PersonRepository;
import org.ironrhino.sample.remoting.TestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemotingConfiguration {

	@Value("${serializationType}")
	private SerializationType serializationType;

	@Bean
	public HttpInvokerClient testService() {
		HttpInvokerClient hic = new HttpInvokerClient();
		hic.setServiceInterface(TestService.class);
		hic.setSerializationType(serializationType);
		hic.setHost("localhost");
		return hic;
	}

	@Bean
	public HttpInvokerClient personRepository() {
		HttpInvokerClient hic = new HttpInvokerClient();
		hic.setServiceInterface(PersonRepository.class);
		hic.setSerializationType(serializationType);
		hic.setHost("localhost");
		return hic;
	}

}
