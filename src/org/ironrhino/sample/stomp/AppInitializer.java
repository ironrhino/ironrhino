package org.ironrhino.sample.stomp;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.core.spring.servlet.InheritedDispatcherServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class AppInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.register(StompConfig.class);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet("stomp", new InheritedDispatcherServlet(ctx));
		dynamic.setAsyncSupported(true);
		dynamic.addMapping("/stomp/*");
		dynamic.setLoadOnStartup(1);
	}

}