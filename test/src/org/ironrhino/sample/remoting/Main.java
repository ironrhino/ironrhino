package org.ironrhino.sample.remoting;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.security.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

	public static void main(String[] args) {
		AppInfo.initialize();
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/ironrhino/sample/remoting/test_without_zk.xml");
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

}
