package org.ironrhino.core.remoting;

import org.ironrhino.sample.ws.endpoint.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WsMain {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/ironrhino/core/remoting/test_ws.xml");
		System.out.println(ctx.getBean(UserService.class).suggestUsername("admin"));
		ctx.close();
	}

}
