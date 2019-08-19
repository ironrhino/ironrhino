package org.ironrhino.sample.remoting;

import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.security.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class Main {

	public static void main(String[] args) {
		AppInfo.initialize();
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		UserService userService = ctx.getBean(UserService.class);
		System.out.println(userService.loadUserByUsername("admin").getUsername());
		int loop = 1000;
		long time = System.currentTimeMillis();
		for (int i = 0; i < loop; i++)
			userService.loadUserByUsername("admin");
		System.out.println("httpinvoker:" + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		ctx.close();
	}

	@Configuration
	static class Config {

		@Bean
		public HttpInvokerClient userService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(UserService.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

	}

}
