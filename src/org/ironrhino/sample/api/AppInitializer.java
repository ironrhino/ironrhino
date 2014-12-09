package org.ironrhino.sample.api;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.rest.RestFilter;
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
		dyn.setAsyncSupported(true);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet("api",
				DispatcherServlet.class);
		dynamic.setInitParameter("contextClass",
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter("contextConfigLocation",
				ApiConfig.class.getName());
		dynamic.addMapping("/api/*");
		dynamic.setAsyncSupported(true);
		dynamic.setLoadOnStartup(1);
	}

}