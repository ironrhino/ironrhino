package org.ironrhino.sample.api;

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