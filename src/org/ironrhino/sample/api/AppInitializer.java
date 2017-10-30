package org.ironrhino.sample.api;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.DelegatingFilter;
import org.ironrhino.core.spring.servlet.InheritedDispatcherServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class AppInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String version = new ApiConfig().getVersion();
		String servletName = "api" + version;
		String servletMapping = "/api" + (StringUtils.isNotBlank(version) ? "/" + version : "") + "/*";
		ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName, InheritedDispatcherServlet.class);
		dynamic.setInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, ApiConfig.class.getName());
		dynamic.addMapping(servletMapping);
		dynamic.setAsyncSupported(true);
		dynamic.setLoadOnStartup(1);
		FilterRegistration.Dynamic dynamicFilter = servletContext.addFilter("restFilter", DelegatingFilter.class);
		dynamicFilter.setAsyncSupported(true);
		dynamicFilter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
	}
}