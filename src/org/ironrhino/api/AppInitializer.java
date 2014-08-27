package org.ironrhino.api;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class AppInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext)
			throws ServletException {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.register(AppConfig.class);
		FilterRegistration.Dynamic dyn = servletContext.addFilter("rest",
				new RestFilter());
		dyn.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false,
				"/api/*");
		DispatcherServlet dispatcherServlet = new DispatcherServlet(ctx);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet("api",
				dispatcherServlet);
		dynamic.addMapping("/api/*");
		dynamic.setLoadOnStartup(1);
	}

}