package org.ironrhino.sample.remoting;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.security.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

	public static void main(String[] args) {
		AppInfo.initialize();
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/ironrhino/sample/remoting/test_without_zk.xml");
		UserService userServiceHessian = (UserService) ctx.getBean("userServiceHessian");
		System.out.println(userServiceHessian.loadUserByUsername("admin").getUsername());
		UserService userServiceHttpInvoker = (UserService) ctx.getBean("userServiceHttpInvoker");
		System.out.println(userServiceHttpInvoker.loadUserByUsername("admin").getUsername());
		int loop = 1000;
		long time = System.currentTimeMillis();
		for (int i = 0; i < loop; i++)
			userServiceHessian.loadUserByUsername("admin");
		System.out.println("hessian:" + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		for (int i = 0; i < loop; i++)
			userServiceHttpInvoker.loadUserByUsername("admin");
		System.out.println("httpinvoker:" + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		ctx.close();
	}

}
