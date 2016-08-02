package org.ironrhino.sample.remoting;

import java.util.Collections;

import org.ironrhino.core.util.AppInfo;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestServiceMain {

	public static void main(String[] args) {
		AppInfo.initialize();
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/ironrhino/sample/remoting/testService.xml");
		TestService testService = ctx.getBean(TestService.class);
		testService.ping();
		System.out.println(testService.echo());
		System.out.println(testService.echo(null));
		System.out.println(testService.echo("test"));
		System.out.println(testService.echoList(Collections.singletonList("list")).get(0));
		System.out.println(testService
				.echoListWithArray(Collections.singletonList(new String[] { "echoWithArrayList" })).get(0)[0]);
		System.out.println(testService.countAndAdd(Collections.singletonList("test"), 2));
		System.out.println(testService.echoArray(new String[] { "array" })[0]);
		System.out.println(testService.loadUserByUsername(null));
		System.out.println(testService.loadUserByUsername("username").getUsername());
		System.out.println(testService.search(null));
		System.out.println(testService.search(""));
		System.out.println(testService.search("username").get(0).getUsername());
		try {
			testService.throwException("this is a message");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {

		}
		ctx.close();
	}

}
