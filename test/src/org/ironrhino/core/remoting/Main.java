package org.ironrhino.core.remoting;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

public class Main {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/ironrhino/core/remoting/test_without_zk.xml");
		UserDetailsService userDetailsServiceHessian = (UserDetailsService) ctx.getBean("userDetailsServiceHessian");
		System.out.println(userDetailsServiceHessian.loadUserByUsername("admin").getUsername());
		UserDetailsService userDetailsServiceHttpInvoker = (UserDetailsService) ctx
				.getBean("userDetailsServiceHttpInvoker");
		System.out.println(userDetailsServiceHttpInvoker.loadUserByUsername("admin").getUsername());
		int loop = 1000;
		long time = System.currentTimeMillis();
		for (int i = 0; i < loop; i++)
			userDetailsServiceHessian.loadUserByUsername("admin");
		System.out.println("hessian:" + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		for (int i = 0; i < loop; i++)
			userDetailsServiceHttpInvoker.loadUserByUsername("admin");
		System.out.println("httpinvoker:" + (System.currentTimeMillis() - time) + "ms");
		time = System.currentTimeMillis();
		ctx.close();
	}

}
