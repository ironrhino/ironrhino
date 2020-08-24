package org.ironrhino.sample.tracing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		ExecutorService es = Executors.newFixedThreadPool(1);
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
			Tracing.execute("testAsync", () -> es.execute(Tracing.wrapAsync("testAsyncRunnable", () -> {
				UserService userService = ctx.getBean(UserService.class);
				System.out.println(userService.loadUserByUsername("admin").getUsername());
			})));
		} finally {
			Thread.sleep(2000);
			ctx.close();
			es.shutdown();
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
