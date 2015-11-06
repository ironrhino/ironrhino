package org.ironrhino.sample.ws;

import org.ironrhino.sample.ws.endpoint.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/ironrhino/sample/ws/test.xml");
		System.out.println(ctx.getBean(UserService.class).suggestUsername("admin"));
		ctx.close();
	}

}
