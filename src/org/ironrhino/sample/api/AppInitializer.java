package org.ironrhino.sample.api;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.core.servlet.DelegatingFilter;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class AppInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {

		ServletRegistration.Dynamic dynamic = servletContext.addServlet("api", DispatcherServlet.class);
		dynamic.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter("contextConfigLocation", ApiConfig.class.getName());
		dynamic.addMapping("/api/*");
		dynamic.setAsyncSupported(true);
		dynamic.setLoadOnStartup(1);
		FilterRegistration.Dynamic dynamicFilter = servletContext.addFilter("restFilter", DelegatingFilter.class);
		dynamicFilter.setAsyncSupported(true);
		dynamicFilter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, "api");
	}
}