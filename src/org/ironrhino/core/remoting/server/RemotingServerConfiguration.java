package org.ironrhino.core.remoting.server;

import org.springframework.context.annotation.Bean;

public class RemotingServerConfiguration {

	@Bean(name = "/httpinvoker/*")
	public HttpInvokerServer httpInvokerServer() {
		return new HttpInvokerServer();
	}

}