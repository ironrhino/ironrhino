package org.ironrhino.rest;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.DelegatingFilter;
import org.ironrhino.core.spring.servlet.InheritedDispatcherServlet;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@SuppressWarnings("unchecked")
public abstract class BaseApiInitializer<T extends ApiConfigBase> implements WebApplicationInitializer {

	private Class<T> apiConfigClass;

	public BaseApiInitializer() {
		Class<T> clazz = (Class<T>) ReflectionUtils.getGenericClass(getClass());
		if (clazz != null)
			apiConfigClass = clazz;
		else
			throw new IllegalArgumentException("apiConfigClass should not be null");
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String version = "";
		try {
			version = apiConfigClass.getConstructor().newInstance().getVersion();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
		}
		String servletName = "api" + (StringUtils.isNotBlank(version) ? version : "");
		String servletMapping = "/api" + (StringUtils.isNotBlank(version) ? "/" + version : "") + "/*";
		ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName, InheritedDispatcherServlet.class);
		dynamic.setInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, apiConfigClass.getName());
		dynamic.addMapping(servletMapping);
		dynamic.setAsyncSupported(true);
		dynamic.setLoadOnStartup(1);
		String filterName = "restFilter";
		FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);
		if (filterRegistration == null) {
			FilterRegistration.Dynamic dynamicFilter = servletContext.addFilter("restFilter", DelegatingFilter.class);
			dynamicFilter.setAsyncSupported(true);
			dynamicFilter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
		} else {
			filterRegistration.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
		}
	}
}