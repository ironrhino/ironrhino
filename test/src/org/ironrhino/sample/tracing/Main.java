package org.ironrhino.sample.tracing;

import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.tracing.TracingConfiguration;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.rest.client.RestClientConfiguration;
import org.ironrhino.rest.client.UserClient;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.security.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		AppInfo.initialize();
		AppInfo.setAppName("ironrino-client");
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				RemotingClientConfiguration.class, RestClientConfiguration.class, TracingConfiguration.class);
		try {
			Tracing.execute("testRemotingService", () -> {
				UserService userService = ctx.getBean(UserService.class);
				System.out.println(userService.loadUserByUsername("admin").getUsername());
			});
			Tracing.execute("testAsyncRemotingService", () -> {
				TestService testService = ctx.getBean(TestService.class);
				try {
					System.out.println(testService.loadFutureUserByUsername("admin", TestService.FutureType.COMPLETABLE)
							.get().getUsername());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			Tracing.execute("testRestClient", () -> {
				UserClient userClient = ctx.getBean(UserClient.class);
				System.out.println(userClient.get("admin").getUsername());
			});
		} finally {
			Thread.sleep(2000);
			ctx.close();
		}
	}

	@Configuration
	static class RemotingClientConfiguration {

		@Bean
		public HttpInvokerClient userService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(UserService.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

		@Bean
		public HttpInvokerClient testService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(TestService.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

	}

}
