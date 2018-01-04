package org.ironrhino.sample.websocket;

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
		ctx.register(WebSocketConfig.class);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet("websocket",
				new InheritedDispatcherServlet(ctx));
		dynamic.setAsyncSupported(true);
		dynamic.addMapping("/websocket/*");
		dynamic.setLoadOnStartup(1);
	}

}