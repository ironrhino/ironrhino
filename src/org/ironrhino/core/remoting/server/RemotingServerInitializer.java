package org.ironrhino.core.remoting.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.core.spring.servlet.InheritedDispatcherServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class RemotingServerInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String servletName = "remoting";
		ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName, InheritedDispatcherServlet.class);
		dynamic.setInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, RemotingServerConfiguration.class.getName());
		dynamic.addMapping("/remoting/*");
		dynamic.setLoadOnStartup(1);
	}

}