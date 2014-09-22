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
		FilterRegistration.Dynamic dyn = servletContext.addFilter("rest",
				RestFilter.class);
		dyn.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false,
				"/api/*");
		ServletRegistration.Dynamic dynamic = servletContext.addServlet("api",
				DispatcherServlet.class);
		dynamic.setInitParameter("contextClass",
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter("contextConfigLocation",
				AppConfig.class.getName());
		dynamic.addMapping("/api/*");
		dynamic.setLoadOnStartup(1);
	}

}