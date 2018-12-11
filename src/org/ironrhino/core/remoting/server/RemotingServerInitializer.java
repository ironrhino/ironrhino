package org.ironrhino.core.remoting.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.core.spring.servlet.InheritedDispatcherServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class RemotingServerInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String servletName = HttpInvokerServer.class.getName();
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setId(servletName);
		ctx.register(RemotingServerConfiguration.class);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName,
				new InheritedDispatcherServlet(ctx));
		dynamic.addMapping("/remoting/*");
		dynamic.setAsyncSupported(true);
		dynamic.setLoadOnStartup(Integer.MAX_VALUE - 1);
	}

}